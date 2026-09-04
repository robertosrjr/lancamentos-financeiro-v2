# ADR-0003: Protecoes de Resiliencia na Entrada HTTP

- Status: Accepted
- Date: 2026-09-04

## Context

Os endpoints de escrita podem ser sobrecarregados por rajadas de requisicoes ou payloads excessivos. Ainda nao existem chamadas externas que justifiquem Circuit Breaker, Retry ou Time Limiter.

## Decision

Proteger `POST /api/v1/lancamentos` com um filtro servlet local:

- Limitar a 30 requisicoes por minuto por endereco remoto.
- Restringir payloads JSON a 64 KiB.
- Retornar `429` para excesso de taxa.
- Retornar `413` para payload acima do limite.
- Medir rejeicoes com `app.http.rate_limited` e `app.http.payload_rejected`.
- Limitar o mapa de clientes rastreados a 10.000 entradas e remover janelas expiradas.

## Alternatives Considered

- Rate limiter distribuido: reservado para quando houver varias instancias e um armazenamento compartilhado.
- Resilience4j Circuit Breaker/Retry: nao aplicavel sem dependencias externas.
- Limite somente por `Content-Length`: insuficiente quando o tamanho e enviado por streaming; por isso o stream tambem e limitado.
- Aplicar o limite a todos os endpoints: rejeitado para nao afetar health checks e leituras sem necessidade.

## Consequences

Positive:

- Rajadas de escrita e payloads grandes deixam de consumir recursos indefinidamente.
- Rejeicoes sao observaveis por metricas.
- O limite de estado local evita crescimento ilimitado do controle de clientes.

Negative:

- O limite e por instancia e nao protege corretamente um deployment horizontal sem proxy ou store distribuido.
- Enderecos atras de NAT compartilham a mesma cota.
- Os valores 30/minuto e 64 KiB precisam ser calibrados com trafego real.

## Future Work

Migrar o rate limiting para gateway ou store distribuido quando a aplicacao rodar em multiplas instancias.
