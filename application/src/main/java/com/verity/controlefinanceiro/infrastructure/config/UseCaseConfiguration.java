package com.verity.controlefinanceiro.infrastructure.config;

import com.verity.controlefinanceiro.application.port.in.ConsultarLancamentosUseCase;
import com.verity.controlefinanceiro.application.port.in.EstornarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.application.usecase.ConsultarLancamentosUseCaseImpl;
import com.verity.controlefinanceiro.application.usecase.EstornarLancamentoUseCaseImpl;
import com.verity.controlefinanceiro.application.usecase.RegistrarLancamentoUseCaseImpl;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public RegistrarLancamentoUseCase registrarLancamentoUseCase(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry,
        MeterRegistry meterRegistry
    ) {
        return new RegistrarLancamentoUseCaseImpl(repository, observationRegistry, meterRegistry);
    }

    @Bean
    public ConsultarLancamentosUseCase consultarLancamentosUseCase(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry
    ) {
        return new ConsultarLancamentosUseCaseImpl(repository, observationRegistry);
    }

    @Bean
    public EstornarLancamentoUseCase estornarLancamentoUseCase(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry,
        MeterRegistry meterRegistry
    ) {
        return new EstornarLancamentoUseCaseImpl(repository, observationRegistry, meterRegistry);
    }
}
