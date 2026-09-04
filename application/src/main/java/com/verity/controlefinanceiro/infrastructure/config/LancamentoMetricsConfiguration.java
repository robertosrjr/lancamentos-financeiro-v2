package com.verity.controlefinanceiro.infrastructure.config;

import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LancamentoMetricsConfiguration {

    @Bean
    Gauge lancamentosTotalGauge(LancamentoRepository repository, MeterRegistry meterRegistry) {
        return Gauge.builder("app.lancamentos.total", repository, value -> repository.findAll().size())
            .description("Quantidade atual de lancamentos armazenados")
            .register(meterRegistry);
    }

    @Bean
    Gauge lancamentosAtivosGauge(LancamentoRepository repository, MeterRegistry meterRegistry) {
        return Gauge.builder("app.lancamentos.ativos", repository, value -> repository.findAll().stream()
                .filter(lancamento -> lancamento.status() == StatusLancamento.ATIVO)
                .count())
            .description("Quantidade atual de lancamentos ativos")
            .register(meterRegistry);
    }
}