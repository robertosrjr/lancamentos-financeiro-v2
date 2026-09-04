# Controle Financeiro - v2

[![Cobertura JaCoCo](https://img.shields.io/badge/cobertura-JaCoCo-blue)](#cobertura) | ![Claude](https://img.shields.io/badge/claude-%23D97757.svg?style=for-the-badge&logo=claude&logoColor=white))

Aplicacao de controle de fluxo de caixa construida como uma API REST em Spring Boot. Este documento registra o processo de construcao, as decisoes tecnicas, os agentes e skills utilizados, os controles de observabilidade e resiliencia e as evidencias de validacao.

> Este README segue o principio de **Diligencia**: toda decisao deve partir de evidencia observavel, declarar suas premissas, registrar riscos e ser validada por uma verificacao executavel sempre que possivel.

## Sumario

- [Objetivo](#objetivo)
- [Estado atual](#estado-atual)
- [Arquitetura](#arquitetura)
- [Como o projeto foi construido](#como-o-projeto-foi-construido)
- [Agentes e skills](#agentes-e-skills)
- [Observabilidade](#observabilidade)
- [Resiliencia](#resiliencia)
- [LGPD e privacidade](#lgpd-e-privacidade)
- [Seguranca](#seguranca)
- [Como executar](#como-executar)
- [Como validar](#como-validar)
- [ADRs](#adrs)
- [Limitacoes e proximos passos](#limitacoes-e-proximos-passos)

## Objetivo

A API gerencia lancamentos financeiros de credito e debito, permitindo:

- Registrar um lancamento.
- Listar lancamentos.
- Buscar um lancamento por identificador.
- Estornar um lancamento.
- Gerar evento de outbox para o registro.
- Expor health checks e metricas operacionais.

O dominio utiliza BRL, valida dados obrigatorios e impede lancamentos com data futura.

## Estado atual

| Item | Estado |
| --- | --- |
| Java configurado no Maven | 21 |
| Spring Boot | 4.1.1 |
| Build | Maven 3.9.11 |
| Persistencia | Repositorio em memoria concorrente |
| API | Spring MVC REST |
| Validacao | Jakarta Bean Validation |
| Seguranca | HTTP Basic e autorizacao por rota |
| Logs | SLF4J, campos estruturados e correlacao |
| Tracing | Micrometer Observation com OpenTelemetry/OTLP |
| Metricas | Actuator, HTTP, resiliencia e metricas de negocio |
| Idempotencia | Chave deterministica e replay seguro local |
| Testes | Unitarios, MockMvc, resiliencia, metricas e ArchUnit |
| Cobertura | JaCoCo no ciclo `verify` |
| ADRs | Seis decisoes registradas |

## Arquitetura

O projeto usa uma organizacao inspirada em Hexagonal Architecture:

```text
src/main/java/com/verity/controlefinanceiro
├── Application.java
├── application
│   ├── port/in       Contratos dos casos de uso
│   ├── port/out      Contratos de persistencia e outbox
│   └── usecase       Regras de aplicacao
├── domain
│   ├── exception     Excecoes de dominio
│   └── model         Lancamento, Money e enumeracoes
└── infrastructure
    ├── adapter/in/web       Controllers e filtros HTTP
    ├── adapter/out/persistence Repositorio atual
    └── config               Beans, seguranca e metricas
```

### Fluxo principal de registro

1. O controller recebe e valida a requisicao.
2. O caso de uso calcula o payload deterministico.
3. Uma chave SHA-256 e gerada com o conteudo e o usuario.
4. O outbox e consultado para detectar replay.
5. Se a chave ja existir, o lancamento original e retornado.
6. Caso contrario, o lancamento e salvo.
7. Um evento `LancamentoRegistrado` e salvo no outbox.
8. Logs, metricas e uma Observation registram o resultado tecnico.

## Como o projeto foi construido

### 1. Reconhecimento inicial

O processo comecou identificando:

- O sistema de build e a raiz efetiva da aplicacao.
- O `pom.xml` e as dependencias diretas.
- O JDK e Maven disponiveis.
- As classes de entrada, casos de uso, dominio, persistencia e testes.
- Arquivos de configuracao e possiveis pipelines CI/CD.

A evidencia mostrou um projeto Maven de modulo unico, sem wrapper Maven, sem pipeline CI/CD no escopo analisado e com persistencia em memoria.

### 2. Hipotese local e verificacao barata

Antes de cada alteracao, foi formulada uma hipotese pequena e verificavel, por exemplo:

- A idempotencia existia apenas como hash e nao impedia duplicacao.
- O erro de startup era causado pela porta, nao pelo contexto Spring.
- O teste de configuracao estava incompatível com a nova injecao de `ObservationRegistry`.

As hipoteses foram verificadas por leitura localizada, busca de referencias, compilacao focada, testes especificos ou startup direto do JAR.

### 3. Implementacao incremental

As mudancas foram aplicadas em fatias pequenas:

- Dependencias de tracing.
- Configuracao de logs e OTLP.
- Logs no controller e nos casos de uso.
- Spans de operacoes de negocio.
- Rate limiter e limite de payload.
- Metricas de negocio.
- Idempotencia real.
- ADRs e documentacao.

Depois de cada alteracao foi feita uma verificacao focada antes de ampliar o escopo.

### 4. Revisao de necessidade e suficiência

Cada implementacao foi revisada com duas perguntas:

- **Suficiencia:** todos os requisitos da decisao foram atendidos?
- **Necessidade:** existe alguma mudanca que nao altera o comportamento ou nao protege uma necessidade real?

Foram evitados Circuit Breaker e Retry nos casos de uso atuais porque nao existem chamadas externas. Retry cego em operacao financeira poderia duplicar lancamentos.

## Agentes e skills

Os arquivos em `.claude/agents` e `.claude/skills` sao definicoes declarativas e guias de trabalho. Eles nao sao executaveis como processos isolados; foram usados como criterios tecnicos para analise, implementacao e revisao.

### Agentes

| Agente | Responsabilidade aplicada |
| --- | --- |
| `spring-logging-agent.md` | Logs estruturados, niveis adequados e protecao contra PII. |
| `spring-metrics-agent.md` | Metricas Micrometer, sinais de ouro e controle de cardinalidade. |
| `spring-tracing-agent.md` | Micrometer Tracing, OpenTelemetry, correlacao e propagacao W3C. |
| `sre-observability-agent.md` | Revisao integrada de logs, metricas, tracing e confiabilidade. |
| `resilience-checker-agent.md` | Identificacao de falhas e avaliacao de Retry, Circuit Breaker, limites e idempotencia. |
| `lgpd-sre-compliance-agent.md` | Minimizacao, sanitizacao, menor privilegio e privacidade operacional. |

### Skills

| Skill | Uso no projeto |
| --- | --- |
| `spring-logging-skill` | Definiu formato ECS, correlacao e regras de nao registrar payloads. |
| `spring-metrics-skill` | Orientou Counters, Gauges e tags de baixa cardinalidade. |
| `spring-tracing-skill` | Orientou Observations, OTLP e contexto de trace. |
| `sre-observability-skill` | Aplicou os Quatro Sinais de Ouro e revisao SRE. |
| `resilience-checker-skill` | Avaliou protecoes HTTP, idempotencia e limites de recursos. |
| `lgpd-sre-compliance-skill` | Aplicou minimizacao de dados e controles de telemetria. |

## Observabilidade

### Logs

Os logs usam SLF4J com campos estruturados e sao enviados para stdout pelo framework. Os pontos registrados incluem:

- Requisicoes HTTP concluídas: metodo, rota, status, duracao e `requestId`.
- Registro, consulta e estorno: eventos e identificadores tecnicos.
- Rejeicoes de negocio: tipo tecnico do erro e status HTTP.
- Persistencia: eventos `DEBUG` para diagnostico do repositorio em memoria.

Os logs nao registram corpo da requisicao, corpo da resposta, valor, descricao, categoria ou `usuarioId`.

### Tracing

Dependencias utilizadas no `pom.xml`:

- `micrometer-tracing-bridge-otel`.
- `opentelemetry-exporter-otlp`.

Configuracoes principais em `src/main/resources/application.yml`:

- Amostragem configuravel por `TRACING_SAMPLING_PROBABILITY`, padrao `0.1`.
- Endpoint configuravel por `OTEL_EXPORTER_OTLP_ENDPOINT`, padrao local `http://localhost:4318`.
- Correlacao com `traceId`, `spanId` e `requestId`.

As operacoes de registrar, consultar e estornar criam Observations com nomes estaveis e tags de baixa cardinalidade.

### Metricas

Metricas personalizadas implementadas:

| Nome | Tipo | Significado |
| --- | --- | --- |
| `app.lancamentos.registrados` | Counter | Novos lancamentos criados. |
| `app.lancamentos.estornados` | Counter | Estornos concluidos. |
| `app.lancamentos.total` | Gauge | Lancamentos atuais no repositorio. |
| `app.lancamentos.ativos` | Gauge | Lancamentos atuais com status ativo. |
| `app.http.rate_limited` | Counter | Requisicoes bloqueadas por excesso de taxa. |
| `app.http.payload_rejected` | Counter | Payloads JSON rejeitados por tamanho. |

O Actuator expoe `health`, `info` e `metrics`. A tag global `application` identifica o servico sem introduzir alta cardinalidade.

## Resiliencia

### Protecoes implementadas

`RequestResilienceFilter` protege `POST /api/v1/lancamentos` com:

- 30 requisicoes por minuto por endereco remoto.
- Payload JSON limitado a 64 KiB.
- Resposta `429` para excesso de taxa.
- Resposta `413` para payload excessivo.
- Limite de 10.000 clientes rastreados e limpeza de janelas expiradas.
- Contadores Micrometer para rejeicoes.

A idempotencia do registro:

- Reutiliza a chave deterministica no outbox.
- Retorna o lancamento original em reexecucao.
- Evita novo lancamento e novo evento no replay normal.
- Serializa o fluxo no repositorio em memoria para reduzir corrida local.

### Padroes deliberadamente nao aplicados

- **Circuit Breaker:** nao ha dependencia externa.
- **Retry:** nao deve repetir escrita financeira sem uma fronteira idempotente persistente.
- **Time Limiter:** nao ha chamada assincrona ou remota.
- **Bulkhead:** nao ha pool externo ou recurso compartilhado remoto.
- **Cache:** a consulta atual e simples e a fonte e em memoria.

## LGPD e privacidade

A estrategia de privacidade e aplicar minimizacao na origem:

- Nenhum payload financeiro e enviado para logs ou traces.
- IDs sao usados somente quando necessarios para correlacao operacional.
- Nenhum identificador de usuario e usado como tag de metrica.
- Nao ha logging de query string ou corpo HTTP.
- A telemetria e estruturada para permitir RBAC, retencao e auditoria no coletor.
- Ambientes de teste devem usar dados anonimizados ou sinteticos.

A aplicacao nao substitui controles de infraestrutura. Em producao ainda devem ser definidos acesso por menor privilegio, retencao, auditoria de consultas e anonimização de backups.

## Seguranca

`SecurityConfig` aplica:

- HTTP Basic para rotas protegidas.
- `/actuator/health` e `/h2-console/**` liberados.
- Demais rotas exigem autenticacao.
- CSRF desabilitado para a API stateless atual.
- `frameOptions.sameOrigin()` para permitir o console H2 local.

Antes de producao, recomenda-se substituir credenciais basicas por um mecanismo de identidade adequado, proteger ou remover o console H2 e revisar a exposicao dos endpoints Actuator.

## Como executar

Pre-requisitos:

- JDK 21.
- Maven 3.9 ou superior.

Na pasta `application`:

```powershell
mvn spring-boot:run
```

Por padrao, a aplicacao usa a porta `8080`. Se ela estiver ocupada:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Variaveis opcionais:

```powershell
$env:TRACING_SAMPLING_PROBABILITY = "0.1"
$env:OTEL_EXPORTER_OTLP_ENDPOINT = "http://localhost:4318"
```

## Como validar

Compilar codigo principal e testes:

```powershell
mvn clean test-compile
```

Executar toda a suite:

```powershell
mvn clean test
```

Executar testes focados:

```powershell
mvn "-Dtest=RegistrarLancamentoUseCaseImplTest,UseCaseConfigurationTest,RequestResilienceFilterTest" test
```

Validar o pacote completo:

```powershell
mvn clean verify -Djacoco.skip=false
```

### Cobertura

[![Relatorio JaCoCo](https://img.shields.io/badge/relat%C3%B3rio-JaCoCo-blue)](application/target/site/jacoco/index.html)

O percentual nao e fixado no README enquanto nao houver publicacao automatica do relatorio em CI. Gere a cobertura localmente com:

```text
application/target/site/jacoco/index.html
```

### Estrategia de testes

Os testes seguem a piramide recomendada pelo agente de QA:

- **Dominio:** regras de `Money` e `Lancamento`, sem Spring.
- **Casos de uso:** registro, consulta, estorno, erros e replay idempotente.
- **Concorrencia:** oito chamadas simultaneas do mesmo comando produzem um unico lancamento e um unico evento outbox.
- **Web:** controller com MockMvc para criar, listar, buscar, estornar e rejeitar payload invalido.
- **Resiliencia:** `RequestResilienceFilterTest` verifica `429`, `413`, metricas de rejeicao e bypass de leituras.
- **Metricas:** `LancamentoMetricsConfigurationTest` verifica gauges total e ativos com `SimpleMeterRegistry`.
- **Arquitetura:** `ArchitectureRulesTest` usa ArchUnit para impedir dependencia do dominio em infraestrutura e Spring.

O JaCoCo e executado no `verify` por meio do `jacoco-maven-plugin`. O projeto ainda nao define um limiar minimo que falhe o build; o relatorio deve ser revisado junto com os cenarios e riscos.

Evidencias registradas durante a construcao:

- O baseline anterior passou em compilacao e testes.
- Os testes de idempotencia passaram com 3 testes, 0 falhas e 0 erros.
- Os testes de configuracao e registro passaram apos a injecao de `MeterRegistry`.
- A compilacao das classes de resiliencia e metricas passou.
- Os testes do filtro de resiliencia passaram com 3 cenarios e zero falhas.
- Os cenarios medios adicionaram busca, estorno, validacao HTTP e gauges de negocio.
- Os cenarios de baixa prioridade adicionaram concorrencia de idempotencia e regras ArchUnit.
- O JaCoCo foi configurado para gerar relatorio no ciclo `verify`.
- O startup direto revelou que uma falha em `spring-boot:run` pode ser simplesmente a porta `8080` ocupada.

## ADRs

As decisoes arquiteturais estao em [adrs/README.md](adrs/README.md):

- [ADR-0001: Observabilidade com Logs Estruturados e Tracing](adrs/0001-observability-logs-and-tracing.md)
- [ADR-0002: Metricas de Negocio com Micrometer](adrs/0002-business-metrics-with-micrometer.md)
- [ADR-0003: Protecoes de Resiliencia na Entrada HTTP](adrs/0003-http-resilience-guards.md)
- [ADR-0004: Idempotencia no Registro e Chave do Outbox](adrs/0004-idempotency-and-outbox-key.md)
- [ADR-0005: Minimizacao de Dados Pessoais na Telemetria](adrs/0005-lgpd-telemetry-data-minimization.md)
- [ADR-0006: Estrategia de Testes, Regras Arquiteturais e Cobertura](adrs/0006-test-strategy-and-coverage.md)

## Limitacoes e proximos passos

1. Substituir o repositorio em memoria por banco persistente.
2. Tornar lancamento e outbox atomicos em uma mesma transacao.
3. Criar um publicador de outbox com retry idempotente e backoff.
4. Mover rate limiting para gateway ou armazenamento distribuido em ambiente horizontal.
5. Calibrar os limites de 30 requisicoes por minuto e 64 KiB com trafego real.
6. Adicionar smoke test do contexto Spring e testes de seguranca dos endpoints.
7. Adicionar metricas de eventos outbox pendentes quando existir publicador.
8. Configurar coleta, retencao, RBAC e auditoria da telemetria.
9. Reavaliar a meta de Java 25: o `pom.xml` atualmente esta em Java 21.
10. Remover ou proteger o console H2 antes de qualquer ambiente produtivo.
11. Definir limiar JaCoCo depois de medir a cobertura real e revisar falsos incentivos.

## Licenca e contribuicao

Este repositorio e um projeto de estudo. Mudancas devem preservar as regras de privacidade, idempotencia, observabilidade e validacao descritas neste documento e nos ADRs.
