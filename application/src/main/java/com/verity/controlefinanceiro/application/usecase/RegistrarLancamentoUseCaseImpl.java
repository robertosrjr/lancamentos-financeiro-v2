package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.application.port.out.OutboxEvent;
import com.verity.controlefinanceiro.domain.model.CategoriaLancamento;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;
import java.util.UUID;

public class RegistrarLancamentoUseCaseImpl implements RegistrarLancamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(RegistrarLancamentoUseCaseImpl.class);

    private final LancamentoRepository repository;
    private final ObservationRegistry observationRegistry;
    private final Counter lancamentosRegistrados;

    public RegistrarLancamentoUseCaseImpl(LancamentoRepository repository) {
        this(repository, ObservationRegistry.NOOP);
    }

    public RegistrarLancamentoUseCaseImpl(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry
    ) {
        this(repository, observationRegistry, new SimpleMeterRegistry());
    }

    public RegistrarLancamentoUseCaseImpl(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry,
        MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.observationRegistry = observationRegistry;
        this.lancamentosRegistrados = Counter.builder("app.lancamentos.registrados")
            .description("Total de lancamentos registrados")
            .register(meterRegistry);
    }

    @Override
    public Lancamento registrar(RegistrarLancamentoCommand command) {
        Observation observation = Observation.createNotStarted("lancamento.registrar", observationRegistry)
            .lowCardinalityKeyValue("operation", "registrar");
        observation.start();

        try {
            Lancamento saved = registrarLancamento(command);
            observation.stop();
            return saved;
        } catch (RuntimeException exception) {
            observation.error(exception);
            observation.stop();
            throw exception;
        }
    }

    private synchronized Lancamento registrarLancamento(RegistrarLancamentoCommand command) {
        Money money = new Money(command.valor(), Currency.getInstance("BRL"));
        String categoriaNormalizada = CategoriaLancamento.normalizar(command.categoria());

        String payload = String.format(
            "{\"tipo\":\"%s\",\"valor\":\"%s\",\"data\":\"%s\",\"descricao\":\"%s\",\"categoria\":\"%s\"}",
            command.tipo(),
            money.amount(),
            command.data(),
            command.descricao(),
            categoriaNormalizada == null ? "" : categoriaNormalizada
        );
        String idempotencyKey = hash(payload + "|usuarioId=" + command.usuarioId());

        Lancamento existing = repository.findOutboxEventByIdempotencyKey(idempotencyKey)
            .map(event -> event.aggregateId())
            .flatMap(repository::findById)
            .orElse(null);

        if (existing != null) {
            logger.atInfo()
                .addKeyValue("event", "lancamento.registration.replayed")
                .addKeyValue("lancamentoId", existing.id())
                .log("Lancamento registration replayed safely");
            return existing;
        }

        Lancamento lancamento = new Lancamento(
            UUID.randomUUID(),
            command.tipo(),
            money,
            command.data(),
            command.descricao(),
            command.categoria(),
            command.usuarioId(),
            StatusLancamento.ATIVO,
            null,
            idempotencyKey
        );

        Lancamento saved = repository.save(lancamento);

        repository.saveOutboxEvent(OutboxEvent.create(
            saved.id(),
            "Lancamento",
            "LancamentoRegistrado",
            payload,
            idempotencyKey
        ));

        logger.atInfo()
            .addKeyValue("event", "lancamento.created")
            .addKeyValue("lancamentoId", saved.id())
            .addKeyValue("type", saved.tipo())
            .log("Lancamento created");
        lancamentosRegistrados.increment();

        return saved;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível no runtime", e);
        }
    }
}
