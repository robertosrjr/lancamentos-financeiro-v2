package com.verity.controlefinanceiro.application.usecase;

import com.verity.controlefinanceiro.application.port.in.ConsultarLancamentosUseCase;
import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class ConsultarLancamentosUseCaseImpl implements ConsultarLancamentosUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ConsultarLancamentosUseCaseImpl.class);

    private final LancamentoRepository repository;
    private final ObservationRegistry observationRegistry;

    public ConsultarLancamentosUseCaseImpl(LancamentoRepository repository) {
        this(repository, ObservationRegistry.NOOP);
    }

    public ConsultarLancamentosUseCaseImpl(
        LancamentoRepository repository,
        ObservationRegistry observationRegistry
    ) {
        this.repository = repository;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public List<Lancamento> listarTodos() {
        Observation observation = Observation.createNotStarted("lancamento.listar", observationRegistry)
            .lowCardinalityKeyValue("operation", "listar");
        observation.start();
        try {
            List<Lancamento> lancamentos = repository.findAll();
            logger.atDebug()
                .addKeyValue("event", "lancamento.listed")
                .addKeyValue("resultCount", lancamentos.size())
                .log("Lancamentos loaded");
            return lancamentos;
        } catch (RuntimeException exception) {
            observation.error(exception);
            throw exception;
        } finally {
            observation.stop();
        }
    }

    @Override
    public Lancamento buscarPorId(UUID id) {
        Observation observation = Observation.createNotStarted("lancamento.buscar", observationRegistry)
            .lowCardinalityKeyValue("operation", "buscarPorId");
        observation.start();
        try {
            return repository.findById(id).orElseThrow(() -> {
                logger.atWarn()
                    .addKeyValue("event", "lancamento.not_found")
                    .addKeyValue("lancamentoId", id)
                    .log("Lancamento not found");
                return new IllegalArgumentException("Lançamento não encontrado: " + id);
            });
        } catch (RuntimeException exception) {
            observation.error(exception);
            throw exception;
        } finally {
            observation.stop();
        }
    }
}
