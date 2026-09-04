package com.verity.controlefinanceiro.infrastructure.adapter.out.persistence;

import com.verity.controlefinanceiro.application.port.out.OutboxEvent;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLancamentoRepositoryTest {

    @Test
    void should_save_and_find_lancamento_by_id() {
        InMemoryLancamentoRepository repository = new InMemoryLancamentoRepository();
        UUID id = UUID.randomUUID();
        Lancamento lancamento = new Lancamento(
            id,
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("150.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 30),
            "Venda",
            "Vendas"
        );

        Lancamento saved = repository.save(lancamento);

        assertThat(saved).isEqualTo(lancamento);
        assertThat(repository.findById(id)).isEqualTo(Optional.of(lancamento));
    }

    @Test
    void should_list_all_saved_lancamentos() {
        InMemoryLancamentoRepository repository = new InMemoryLancamentoRepository();
        Lancamento primeiro = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("50.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 28),
            "Primeira venda",
            "Vendas"
        );
        Lancamento segundo = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("30.00"), Currency.getInstance("BRL")),
            LocalDate.of(2026, 8, 29),
            "Compra",
            "Compras"
        );

        repository.save(primeiro);
        repository.save(segundo);

        List<Lancamento> result = repository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(primeiro, segundo);
    }

    @Test
    void should_save_and_find_outbox_event_by_idempotency_key() {
        InMemoryLancamentoRepository repository = new InMemoryLancamentoRepository();
        UUID aggregateId = UUID.randomUUID();
        String key = "idempotency-123";
        OutboxEvent event = OutboxEvent.create(
            aggregateId,
            "Lancamento",
            "LancamentoRegistrado",
            "{\"tipo\":\"CREDITO\"}",
            key
        );

        OutboxEvent saved = repository.saveOutboxEvent(event);

        assertThat(saved).isEqualTo(event);
        assertThat(repository.findOutboxEventByIdempotencyKey(key)).isEqualTo(Optional.of(event));
    }
}
