package com.verity.controlefinanceiro.infrastructure.config;

import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LancamentoMetricsConfigurationTest {

    @Test
    void should_expose_total_and_active_lancamento_gauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InMemoryRepository repository = new InMemoryRepository();
        Lancamento active = lancamento(StatusLancamento.ATIVO);
        Lancamento reversed = lancamento(StatusLancamento.ESTORNADO);
        repository.items.add(active);
        repository.items.add(reversed);

        LancamentoMetricsConfiguration configuration = new LancamentoMetricsConfiguration();
        configuration.lancamentosTotalGauge(repository, registry);
        configuration.lancamentosAtivosGauge(repository, registry);

        assertThat(registry.get("app.lancamentos.total").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("app.lancamentos.ativos").gauge().value()).isEqualTo(1.0);
    }

    private Lancamento lancamento(StatusLancamento status) {
        return new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Teste",
            "Testes",
            "usuario",
            status,
            null,
            null
        );
    }

    private static class InMemoryRepository implements LancamentoRepository {
        private final List<Lancamento> items = new ArrayList<>();

        @Override public Lancamento save(Lancamento lancamento) { items.add(lancamento); return lancamento; }
        @Override public Optional<Lancamento> findById(UUID id) { return items.stream().filter(item -> item.id().equals(id)).findFirst(); }
        @Override public List<Lancamento> findAll() { return new ArrayList<>(items); }
        @Override public com.verity.controlefinanceiro.application.port.out.OutboxEvent saveOutboxEvent(com.verity.controlefinanceiro.application.port.out.OutboxEvent event) { return event; }
        @Override public Optional<com.verity.controlefinanceiro.application.port.out.OutboxEvent> findOutboxEventByIdempotencyKey(String key) { return Optional.empty(); }
    }
}