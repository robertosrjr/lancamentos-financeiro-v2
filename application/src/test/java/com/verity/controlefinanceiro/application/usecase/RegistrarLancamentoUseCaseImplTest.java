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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void should_create_distinct_lancamentos_for_different_users_with_same_payload() {
        RegistrarLancamentoCommand commandUsuarioA = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("99.90"),
            LocalDate.of(2026, 8, 20),
            "Assinatura mensal",
            "Serviços",
            "usuario-a"
        );
        RegistrarLancamentoCommand commandUsuarioB = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("99.90"),
            LocalDate.of(2026, 8, 20),
            "Assinatura mensal",
            "Serviços",
            "usuario-b"
        );

        Lancamento lancamentoA = useCase.registrar(commandUsuarioA);
        Lancamento lancamentoB = useCase.registrar(commandUsuarioB);

        assertThat(lancamentoA.id()).isNotEqualTo(lancamentoB.id());
        assertThat(lancamentoA.idempotencyKey()).isNotEqualTo(lancamentoB.idempotencyKey());
        assertThat(repository.saved).hasSize(2);
    }

    @Test
    void should_treat_corrected_value_as_new_registration_instead_of_replay() {
        RegistrarLancamentoCommand original = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("50.00"),
            LocalDate.of(2026, 8, 25),
            "Compra de suprimentos",
            "Despesas",
            "usuario-789"
        );
        RegistrarLancamentoCommand corrigido = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("55.00"),
            LocalDate.of(2026, 8, 25),
            "Compra de suprimentos",
            "Despesas",
            "usuario-789"
        );

        Lancamento primeiro = useCase.registrar(original);
        Lancamento segundo = useCase.registrar(corrigido);

        assertThat(segundo.id()).isNotEqualTo(primeiro.id());
        assertThat(segundo.valor().amount()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(repository.saved).hasSize(2);
    }

    @Test
    void should_reject_registration_when_value_has_more_than_two_decimal_places() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("10.999"),
            LocalDate.of(2026, 8, 26),
            "Compra com valor inválido",
            "Compras",
            "usuario-999"
        );

        assertThatThrownBy(() -> useCase.registrar(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("decimal");
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void should_treat_different_decimal_scale_as_same_value_for_idempotency() {
        RegistrarLancamentoCommand comEscalaCurta = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("10.5"),
            LocalDate.of(2026, 8, 28),
            "Compra com escala diferente",
            "Compras",
            "usuario-escala"
        );
        RegistrarLancamentoCommand comEscalaLonga = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("10.50"),
            LocalDate.of(2026, 8, 28),
            "Compra com escala diferente",
            "Compras",
            "usuario-escala"
        );

        Lancamento primeiro = useCase.registrar(comEscalaCurta);
        Lancamento replay = useCase.registrar(comEscalaLonga);

        assertThat(replay).isSameAs(primeiro);
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void should_reject_registration_with_category_outside_catalog() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.CREDITO,
            new BigDecimal("30.00"),
            LocalDate.of(2026, 8, 29),
            "Receita com categoria inválida",
            "Categoria Inexistente",
            "usuario-categoria"
        );

        assertThatThrownBy(() -> useCase.registrar(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("catálogo");
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void should_treat_different_category_casing_as_same_value_for_idempotency() {
        RegistrarLancamentoCommand comCategoriaMinuscula = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("20.00"),
            LocalDate.of(2026, 8, 30),
            "Compra com categoria em caixa diferente",
            "compras",
            "usuario-categoria-casing"
        );
        RegistrarLancamentoCommand comCategoriaCanonica = new RegistrarLancamentoCommand(
            TipoLancamento.DEBITO,
            new BigDecimal("20.00"),
            LocalDate.of(2026, 8, 30),
            "Compra com categoria em caixa diferente",
            "Compras",
            "usuario-categoria-casing"
        );

        Lancamento primeiro = useCase.registrar(comCategoriaMinuscula);
        Lancamento replay = useCase.registrar(comCategoriaCanonica);

        assertThat(replay).isSameAs(primeiro);
        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void should_persist_lancamento_even_when_outbox_event_save_fails() {
        RegistrarLancamentoCommand command = new RegistrarLancamentoCommand(
            TipoLancamento.CREDITO,
            new BigDecimal("120.00"),
            LocalDate.of(2026, 8, 27),
            "Receita com falha de auditoria",
            "Receitas",
            "usuario-outbox-falho"
        );
        repository.failOnNextOutboxEvent = true;

        assertThatThrownBy(() -> useCase.registrar(command))
            .isInstanceOf(RuntimeException.class);

        assertThat(repository.saved).hasSize(1);
        assertThat(repository.outboxEvents).isEmpty();
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
        private boolean failOnNextOutboxEvent = false;

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
            if (failOnNextOutboxEvent) {
                failOnNextOutboxEvent = false;
                throw new RuntimeException("Falha simulada ao publicar evento de auditoria");
            }
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
