package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.in.EstornarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class EstornarLancamentoUseCaseImpl implements EstornarLancamentoUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EstornarLancamentoUseCaseImpl.class);

    private final LancamentoRepository repository;
    private final ObservationRegistry observationRegistry;
    private final Counter lancamentosEstornados;

    public EstornarLancamentoUseCaseImpl(LancamentoRepository repository) {
        this(repository, ObservationRegistry.NOOP);
    }

    public EstornarLancamentoUseCaseImpl(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry
    ) {
        this(repository, observationRegistry, new SimpleMeterRegistry());
    }

    public EstornarLancamentoUseCaseImpl(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry,
        MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.observationRegistry = observationRegistry;
        this.lancamentosEstornados = Counter.builder("app.lancamentos.estornados")
            .description("Total de lancamentos estornados")
            .register(meterRegistry);
    }

    @Override
    public Lancamento estornar(UUID lancamentoId) {
        Observation observation = Observation.createNotStarted("lancamento.estornar", observationRegistry)
            .lowCardinalityKeyValue("operation", "estornar");
        observation.start();
        try {
            Lancamento original = repository.findById(lancamentoId)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + lancamentoId));

            if (original.status().name().equals("ESTORNADO")) {
                logger.atWarn()
                    .addKeyValue("event", "lancamento.reversal.rejected")
                    .addKeyValue("lancamentoId", lancamentoId)
                    .addKeyValue("reason", "already_reversed")
                    .log("Lancamento reversal rejected");
                throw new IllegalStateException("Lançamento já foi estornado");
            }

            if (original.lancamentoOrigemId() != null) {
                logger.atWarn()
                    .addKeyValue("event", "lancamento.reversal.rejected")
                    .addKeyValue("lancamentoId", lancamentoId)
                    .addKeyValue("reason", "reversal_chain_not_allowed")
                    .log("Lancamento reversal rejected");
                throw new IllegalStateException(
                    "Não é permitido estornar um lançamento que já é um estorno; registre um lançamento de correção"
                );
            }

            Lancamento estorno = original.estornar();
            Lancamento saved = repository.save(estorno);
            logger.atInfo()
                .addKeyValue("event", "lancamento.reversed")
                .addKeyValue("lancamentoId", lancamentoId)
                .addKeyValue("reversalId", saved.id())
                .log("Lancamento reversed");
            lancamentosEstornados.increment();
            return saved;
        } catch (RuntimeException exception) {
            observation.error(exception);
            throw exception;
        } finally {
            observation.stop();
        }
    }
}
