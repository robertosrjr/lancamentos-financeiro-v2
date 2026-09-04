# ADR-0004: Idempotencia no Registro e Chave do Outbox

- Status: Accepted
- Date: 2026-09-04

## Context

Uma requisicao repetida pode ocorrer por retry do cliente, timeout ou reenvio de mensagem. Criar um novo lancamento em cada tentativa pode duplicar uma operacao financeira. O dominio ja possui `idempotencyKey` e o outbox armazena o `aggregateId`.

## Decision

Usar um hash deterministico do comando como chave de idempotencia e consultar o outbox antes de persistir:

1. Construir o payload deterministico do comando.
2. Incluir `usuarioId` no material do hash para evitar colisao entre usuarios.
3. Procurar evento existente pela chave.
4. Recuperar e retornar o lancamento associado ao `aggregateId`.
5. Persistir lancamento e evento somente quando a chave ainda nao existir.
6. Serializar o fluxo no caso do repositorio em memoria para evitar duplicacao concorrente local.

A chave nao e enviada como tag de metricas nem o payload e incluido em logs.

## Alternatives Considered

- Retry cego da operacao de escrita: rejeitado porque pode duplicar lancamentos.
- Usar apenas UUID gerado no servidor: rejeitado porque repeticoes nao poderiam ser reconhecidas.
- Consultar somente o lancamento: rejeitado porque o contrato atual nao expoe busca por chave; o outbox ja possui esse indice.

## Consequences

Positive:

- Repeticoes seguras retornam o mesmo lancamento.
- O evento outbox nao e duplicado no fluxo normal.
- O retry do cliente pode ser tolerado sem repetir o efeito financeiro.

Negative:

- A sincronizacao atual protege apenas o processo local.
- O repositorio em memoria nao oferece garantia entre varias instancias.
- O outbox ainda nao possui publicador, retry de entrega ou transacao persistente.

## Future Work

Ao migrar para banco, criar uma restricao unica para a chave, persistir lancamento e outbox na mesma transacao e implementar publicacao com retry idempotente.
