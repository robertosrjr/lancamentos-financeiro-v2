package com.verity.controlefinanceiro.application.port.out;

import com.verity.controlefinanceiro.domain.model.Lancamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoRepository {
    Lancamento save(Lancamento lancamento);
    Optional<Lancamento> findById(UUID id);
    List<Lancamento> findAll();
    OutboxEvent saveOutboxEvent(OutboxEvent event);
    Optional<OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey);
}
