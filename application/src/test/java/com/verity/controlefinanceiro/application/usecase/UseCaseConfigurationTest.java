package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.in.ConsultarLancamentosUseCase;
import com.verity.controlefinanceiro.application.port.in.EstornarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.infrastructure.config.UseCaseConfiguration;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UseCaseConfigurationTest {

    @Test
    void should_create_application_use_cases_from_repository() {
        UseCaseConfiguration config = new UseCaseConfiguration();
        LancamentoRepository repository = new InMemoryLancamentoRepositoryForTest();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        RegistrarLancamentoUseCase registrar = config.registrarLancamentoUseCase(
            repository, ObservationRegistry.NOOP, meterRegistry
        );
        ConsultarLancamentosUseCase consultar = config.consultarLancamentosUseCase(repository, ObservationRegistry.NOOP);
        EstornarLancamentoUseCase estornar = config.estornarLancamentoUseCase(
            repository, ObservationRegistry.NOOP, meterRegistry
        );

        assertThat(registrar).isNotNull();
        assertThat(consultar).isNotNull();
        assertThat(estornar).isNotNull();
    }

    private static class InMemoryLancamentoRepositoryForTest implements LancamentoRepository {
        @Override
        public com.verity.controlefinanceiro.domain.model.Lancamento save(com.verity.controlefinanceiro.domain.model.Lancamento lancamento) {
            return lancamento;
        }

        @Override
        public java.util.Optional<com.verity.controlefinanceiro.domain.model.Lancamento> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<com.verity.controlefinanceiro.domain.model.Lancamento> findAll() {
            return java.util.List.of();
        }

        @Override
        public com.verity.controlefinanceiro.application.port.out.OutboxEvent saveOutboxEvent(com.verity.controlefinanceiro.application.port.out.OutboxEvent event) {
            return event;
        }

        @Override
        public java.util.Optional<com.verity.controlefinanceiro.application.port.out.OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey) {
            return java.util.Optional.empty();
        }
    }
}
