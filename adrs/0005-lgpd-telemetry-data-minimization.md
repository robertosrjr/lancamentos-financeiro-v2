# ADR-0005: Minimizacao de Dados Pessoais na Telemetria

- Status: Accepted
- Date: 2026-09-04

## Context

A aplicacao processa dados de lancamentos financeiros, incluindo descricao, categoria e identificacao do usuario. Logs, metricas e traces sao necessarios para operacao, mas podem criar uma segunda copia de dados pessoais fora do fluxo funcional.

A decisao deve observar os principios de minimizacao, seguranca e prevencao previstos na LGPD, especialmente os principios do Art. 6 e as medidas de seguranca do Art. 46.

## Decision

Aplicar minimizacao de dados na origem da telemetria:

- Nao registrar request bodies, response bodies, query strings ou payloads do outbox em logs.
- Nao registrar valor, descricao, categoria ou `usuarioId` nos eventos de log dos casos de uso e controllers.
- Usar somente identificadores tecnicos necessarios para diagnostico, como `lancamentoId`, `requestId`, `traceId` e `spanId`.
- Usar metricas sem tags de usuario, UUID, descricao, endereco ou qualquer outro valor de alta cardinalidade.
- Enviar logs para stdout em formato estruturado ECS, permitindo coleta centralizada e politicas externas de acesso e retencao.
- Propagar contexto W3C de tracing por Micrometer/OpenTelemetry, sem adicionar dados de negocio aos atributos dos spans.
- Manter a amostragem de tracing configuravel por ambiente.

## Alternatives Considered

- Registrar o payload completo para facilitar suporte: rejeitado por excesso de dados e risco de vazamento.
- Usar `usuarioId` como tag de metrica: rejeitado por alta cardinalidade e exposicao de identificadores.
- Mascarar depois que o log foi escrito: rejeitado porque o dado sensivel ja teria sido exposto no processo de coleta.
- Desabilitar toda telemetria: rejeitado porque prejudica deteccao de incidentes e disponibilidade.

## Consequences

Positive:

- Reduz a duplicacao de dados pessoais em sistemas operacionais.
- Mantem correlacao suficiente para investigar uma requisicao sem acessar o dominio financeiro.
- Facilita politicas centralizadas de RBAC, retencao e auditoria no coletor de logs.
- Evita cardinalidade explosiva nas metricas.

Negative:

- Alguns incidentes exigirao consulta autorizada ao dado funcional original, pois a telemetria nao contem o payload.
- A aplicacao depende de configuracao correta do coletor, controle de acesso e retencao.
- O identificador tecnico ainda pode ser dado pessoal quando puder ser associado a uma pessoa; seu acesso deve ser controlado.

## Operational Requirements

- Restringir acesso aos logs e traces por menor privilegio.
- Definir prazo de retencao conforme finalidade e politica de privacidade.
- Auditar acessos aos sistemas de observabilidade.
- Garantir que ambientes de teste nao usem dados reais sem anonimização ou pseudonimizacao.
- Revisar novos campos de logs, metricas e spans antes de sua introducao.

## Validation

Revisar chamadas de logger, atributos de Observation e tags de MeterRegistry para garantir que nenhum payload financeiro ou identificador de usuario seja adicionado. Testes devem validar comportamento e nao devem depender de dados pessoais reais.
