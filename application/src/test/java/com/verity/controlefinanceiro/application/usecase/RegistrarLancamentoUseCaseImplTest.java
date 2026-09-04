package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase.RegistrarLancamentoCommand;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.application.port.out.OutboxEvent;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrarLancamentoUseCaseImplTest {

    private FakeLancamentoRepository repository;
    private RegistrarLancamentoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeLancamentoRepository();
        useCase = new RegistrarLancamentoUseCaseImpl(repository);
    }

    @Test
    void should_register_lancamento_with_brl_currency_and_saved_values() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("150.50"),
            LocalDate.of(2026, 8, 30),
            "Compra de materiais",
            "Despesas",
            "usuario-123"
        );

        Lancamento saved = useCase.registrar(command);

        assertThat(saved).isNotNull();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.tipo()).isEqualTo(TipoLancamento.DEBITO);
        assertThat(saved.valor().amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(saved.valor().currency()).isEqualTo(Currency.getInstance("BRL"));
        assertThat(saved.data()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(saved.descricao()).isEqualTo("Compra de materiais");
        assertThat(saved.categoria()).isEqualTo("Despesas");
        assertThat(saved.usuarioId()).isEqualTo("usuario-123");
        assertThat(saved.status()).isEqualTo(StatusLancamento.ATIVO);
        assertThat(saved.idempotencyKey()).isNotBlank();
        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.get(0)).isEqualTo(saved);
        assertThat(repository.outboxEvents).hasSize(1);
        assertThat(repository.outboxEvents.get(0).idempotencyKey()).isNotBlank();
        assertThat(repository.outboxEvents.get(0).idempotencyKey()).isEqualTo(saved.idempotencyKey());
    }

    @Test
    void should_generate_stable_idempotency_key_for_lancamento_event() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.CREDITO,
            new BigDecimal("250.00"),
            LocalDate.of(2026, 8, 31),
            "Receita do cliente",
            "Receitas",
            "usuario-456"
        );

        Lancamento saved = useCase.registrar(command);
        String key = repository.outboxEvents.get(0).idempotencyKey();

        assertThat(saved).isNotNull();
        assertThat(key).isEqualTo(repository.findOutboxEventByIdempotencyKey(key).orElseThrow().idempotencyKey());
        assertThat(key).matches("[a-f0-9]{64}");
    }

    @Test
    void should_return_existing_lancamento_when_registration_is_replayed() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.CREDITO,
            new BigDecimal("250.00"),
            LocalDate.of(2026, 8, 31),
            "Receita do cliente",
            "Receitas",
            "usuario-456"
        );

        Lancamento first = useCase.registrar(command);
        Lancamento replay = useCase.registrar(command);

        assertThat(replay).isSameAs(first);
        assertThat(repository.saved).hasSize(1);
        assertThat(repository.outboxEvents).hasSize(1);
    }

    @Test
    void should_create_only_one_lancamento_when_same_command_is_registered_concurrently() throws Exception {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.CREDITO,
            new BigDecimal("300.00"),
            LocalDate.of(2026, 8, 31),
            "Receita concorrente",
            "Receitas",
            "usuario-concorrente"
        );
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Lancamento> results = new ArrayList<>();

        for (int index = 0; index < 8; index++) {
            executor.submit(() -> {
                ready.countDown();
                start.await();
                synchronized (results) {
                    results.add(useCase.registrar(command));
                }
                return null;
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(results).hasSize(8);
        assertThat(results).allSatisfy(result -> assertThat(result).isSameAs(results.get(0)));
        assertThat(repository.saved).hasSize(1);
        assertThat(repository.outboxEvents).hasSize(1);
    }

    private static class FakeLancamentoRepository implements LancamentoRepository {
        private final List<Lancamento> saved = new ArrayList<>();
        private final Map<UUID, Lancamento> byId = new HashMap<>();
        private final List<OutboxEvent> outboxEvents = new ArrayList<>();

        @Override
        public Lancamento save(Lancamento lancamento) {
            saved.add(lancamento);
            byId.put(lancamento.id(), lancamento);
            return lancamento;
        }

        @Override
        public Optional<Lancamento> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public List<Lancamento> findAll() {
            return new ArrayList<>(saved);
        }

        @Override
        public OutboxEvent saveOutboxEvent(OutboxEvent event) {
            outboxEvents.add(event);
            return event;
        }

        @Override
        public Optional<OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey) {
            return outboxEvents.stream()
                .filter(event -> event.idempotencyKey().equals(idempotencyKey))
                .findFirst();
        }
    }
}
