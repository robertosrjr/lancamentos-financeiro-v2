package com.verity.controlefinanceiro.infrastructure.adapter.out.persistence;

import com.verity.controlefinanceiro.application.port.out.LancamentoRepository;
import com.verity.controlefinanceiro.application.port.out.OutboxEvent;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryLancamentoRepository implements LancamentoRepository {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryLancamentoRepository.class);

    private final Map<UUID, Lancamento> storage = new ConcurrentHashMap<>();
    private final Map<String, OutboxEvent> outboxByKey = new ConcurrentHashMap<>();

    @Override
    public Lancamento save(Lancamento lancamento) {
        storage.put(lancamento.id(), lancamento);
        logger.atDebug()
            .addKeyValue("event", "repository.lancamento.saved")
            .addKeyValue("lancamentoId", lancamento.id())
            .log("Lancamento persisted");
        return lancamento;
    }

    @Override
    public Optional<Lancamento> findById(UUID id) {
        Optional<Lancamento> result = Optional.ofNullable(storage.get(id));
        logger.atDebug()
            .addKeyValue("event", "repository.lancamento.lookup")
            .addKeyValue("lancamentoId", id)
            .addKeyValue("found", result.isPresent())
            .log("Lancamento lookup completed");
        return result;
    }

    @Override
    public List<Lancamento> findAll() {
        List<Lancamento> result = new ArrayList<>(storage.values());
        logger.atDebug()
            .addKeyValue("event", "repository.lancamento.list")
            .addKeyValue("resultCount", result.size())
            .log("Lancamentos loaded from repository");
        return result;
    }

    @Override
    public OutboxEvent saveOutboxEvent(OutboxEvent event) {
        outboxByKey.put(event.idempotencyKey(), event);
        logger.atDebug()
            .addKeyValue("event", "repository.outbox.saved")
            .addKeyValue("aggregateId", event.aggregateId())
            .addKeyValue("eventType", event.eventType())
            .log("Outbox event persisted");
        return event;
    }

    @Override
    public Optional<OutboxEvent> findOutboxEventByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(outboxByKey.get(idempotencyKey));
    }
}
