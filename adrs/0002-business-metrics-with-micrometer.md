# ADR-0002: Metricas de Negocio com Micrometer

- Status: Accepted
- Date: 2026-09-04

## Context

Metricas HTTP e JVM nao respondem sozinhas se a aplicacao esta registrando lancamentos, estornando operacoes ou acumulando dados. E necessario medir contadores de eventos e o estado atual do repositorio.

## Decision

Usar Micrometer com nomes estaveis e sem tags de alta cardinalidade.

- `app.lancamentos.registrados` e um `Counter` incrementado somente quando um novo lancamento e criado.
- `app.lancamentos.estornados` e um `Counter` incrementado somente quando um estorno e concluido.
- `app.lancamentos.total` e um `Gauge` calculado a partir do repositorio.
- `app.lancamentos.ativos` e um `Gauge` calculado a partir do status dos lancamentos.
- `app.http.rate_limited` conta requisicoes bloqueadas.
- `app.http.payload_rejected` conta payloads rejeitados.
- A tag global `application` identifica o servico.

## Alternatives Considered

- Registrar usuario, UUID ou descricao como tag: rejeitado por cardinalidade e privacidade.
- Usar apenas logs para contagem: rejeitado por consultas e alertas menos confiaveis.
- Usar gauges para eventos acumulados: rejeitado porque contadores representam melhor taxas e total monotonicamente crescente.

## Consequences

Positive:

- Taxas de registro e estorno podem alimentar dashboards e alertas.
- O estado atual de lancamentos ativos fica consultavel.
- As metricas podem ser expostas pelo Actuator.

Negative:

- Os gauges refletem apenas o repositorio atual em memoria.
- Os dados sao perdidos quando a aplicacao reinicia.
- Em uma futura persistencia real, os gauges deverao usar consultas ou agregacoes do banco.

## Validation

Os testes de configuracao e registro devem continuar passando com `MeterRegistry` real ou `SimpleMeterRegistry` de teste.
