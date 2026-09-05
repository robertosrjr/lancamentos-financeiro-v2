package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstornarLancamentoUseCaseImplTest {

    private FakeLancamentoRepository repository;
    private EstornarLancamentoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeLancamentoRepository();
        useCase = new EstornarLancamentoUseCaseImpl(repository);
    }

    @Test
    void should_estornar_lancamento_and_persist_reversal() {
        Lancamento original = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("120.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 10),
            "Receita",
            "Vendas"
        );
        repository.store(original);

        Lancamento estorno = useCase.estornar(original.id());

        assertThat(estorno).isNotNull();
        assertThat(estorno.tipo()).isEqualTo(TipoLancamento.DEBITO);
        assertThat(estorno.lancamentoOrigemId()).isEqualTo(original.id());
        assertThat(estorno.status()).isEqualTo(StatusLancamento.ATIVO);
        assertThat(original.status()).isEqualTo(StatusLancamento.ESTORNADO);
        assertThat(repository.saved.get(estorno.id())).isEqualTo(estorno);
    }

    @Test
    void should_throw_when_lancamento_not_found() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.estornar(missingId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(missingId.toString());
    }

    @Test
    void should_reject_estorno_of_an_estorno_lancamento() {
        Lancamento original = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("40.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 12),
            "Lançamento indevido",
            "Compras"
        );
        repository.store(original);
        Lancamento primeiroEstorno = useCase.estornar(original.id());
        repository.store(primeiroEstorno);

        assertThatThrownBy(() -> useCase.estornar(primeiroEstorno.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("estorno");
    }

    @Test
    void should_throw_when_lancamento_already_estornado() {
        Lancamento original = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("60.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 11),
            "Despesa",
            "Compras"
        );
        original.marcarComoEstornado();
        repository.store(original);

        assertThatThrownBy(() -> useCase.estornar(original.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("já foi estornado");
    }

    private static class FakeLancamentoRepository implements LancamentoRepository {
        private final Map<UUID, Lancamento> saved = new HashMap<>();

        void store(Lancamento lancamento) {
            saved.put(lancamento.id(), lancamento);
        }

        @Override
        public Lancamento save(Lancamento lancamento) {
            saved.put(lancamento.id(), lancamento);
            return lancamento;
        }

        @Override
        public Optional<Lancamento> findById(UUID id) {
            return Optional.ofNullable(saved.get(id));
        }

        @Override
        public java.util.List<Lancamento> findAll() {
            return new java.util.ArrayList<>(saved.values());
        }

        @Override
        public com.verity.controlefinanceiro.application.port.out.OutboxEvent saveOutboxEvent(com.verity.controlefinanceiro.application.port.out.OutboxEvent event) {
            return event;
        }

        @Override
        public Optional<com.verity.controlefinanceiro.application.port.out.OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }
    }
}
