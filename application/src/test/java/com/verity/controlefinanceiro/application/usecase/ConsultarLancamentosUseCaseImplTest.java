package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarLancamentosUseCaseImplTest {

    private FakeLancamentoRepository repository;
    private ConsultarLancamentosUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeLancamentoRepository();
        useCase = new ConsultarLancamentosUseCaseImpl(repository);
    }

    @Test
    void should_list_all_lancamentos_from_repository() {
        Lancamento primeiro = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("500.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 1),
            "Receita",
            "Vendas"
        );
        Lancamento segundo = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("250.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 2),
            "Compra",
            "Compras"
        );
        repository.data.add(primeiro);
        repository.data.add(segundo);

        List<Lancamento> result = useCase.listarTodos();

        assertThat(result).containsExactly(primeiro, segundo);
    }

    @Test
    void should_find_existing_lancamento_by_id() {
        Lancamento lancamento = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("75.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 8),
            "Pagamento",
            "Receitas"
        );
        repository.data.add(lancamento);

        Lancamento result = useCase.buscarPorId(lancamento.id());

        assertThat(result).isEqualTo(lancamento);
    }

    @Test
    void should_throw_when_lancamento_does_not_exist() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.buscarPorId(missingId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(missingId.toString());
    }

    private static class FakeLancamentoRepository implements LancamentoRepository {
        private final List<Lancamento> data = new ArrayList<>();

        @Override
        public Lancamento save(Lancamento lancamento) {
            data.add(lancamento);
            return lancamento;
        }

        @Override
        public Optional<Lancamento> findById(UUID id) {
            return data.stream()
                .filter(lancamento -> lancamento.id().equals(id))
                .findFirst();
        }

        @Override
        public List<Lancamento> findAll() {
            return new ArrayList<>(data);
        }
        @Override
        public com.verity.controlefinanceiro.application.port.out.OutboxEvent saveOutboxEvent(com.verity.controlefinanceiro.application.port.out.OutboxEvent event) {
            return event;
        }

        @Override
        public Optional<com.verity.controlefinanceiro.application.port.out.OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey) {
            return Optional.empty();
        }    }
}
