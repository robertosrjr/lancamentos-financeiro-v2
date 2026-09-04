# ADR-0001: Observabilidade com Logs Estruturados e Tracing

- Status: Accepted
- Date: 2026-09-04

## Context

A aplicacao precisa permitir diagnostico de requisicoes e operacoes de negocio sem registrar payloads financeiros ou dados pessoais. Logs textuais isolados nao oferecem correlacao suficiente entre requisicao HTTP e operacao interna.

## Decision

Adotar SLF4J com campos estruturados, logs em stdout e Micrometer Observation com bridge OpenTelemetry.

- `HttpRequestLoggingFilter` registra metodo, rota, status, duracao e `requestId`.
- Controllers e casos de uso registram somente eventos e identificadores tecnicos.
- Operacoes de registro, consulta e estorno criam observations com nomes estaveis.
- `traceId`, `spanId` e `requestId` sao incluidos na correlacao dos logs.
- Exportacao OTLP e configuravel por `OTEL_EXPORTER_OTLP_ENDPOINT`.
- Amostragem e configuravel por `TRACING_SAMPLING_PROBABILITY`, com padrao de 10%.

## Alternatives Considered

- Logs manuais sem campos estruturados: rejeitados por baixa capacidade de correlacao.
- Acoplamento direto ao SDK OpenTelemetry: rejeitado para preservar a abstracao Micrometer/Spring.
- Registrar request/response bodies: rejeitado por minimizacao de dados e risco de exposicao.

## Consequences

Positive:

- Requisicoes podem ser correlacionadas com spans e logs.
- Eventos de negocio sao observaveis sem expor valor, descricao, categoria ou usuario.
- O backend de tracing pode ser trocado por configuracao.

Negative:

- Sem um collector OTLP ativo, spans nao serao exportados externamente.
- A amostragem de 10% nao captura todas as requisicoes.
- O filtro de requisicoes adiciona pequeno custo de medicao por chamada.

## Validation

A compilacao Maven e os diagnosticos Java devem permanecer limpos; testes de casos de uso validam o comportamento sem depender de um backend OTLP.
