# ADR-0006: Estrategia de Testes, Regras Arquiteturais e Cobertura

- Status: Accepted
- Date: 2026-09-04

## Context

A aplicacao evoluiu de testes unitarios de dominio e casos de uso para incluir filtros de resiliencia, metricas de negocio, idempotencia concorrente e regras arquiteturais. Sem uma estrategia explicita, mudancas de infraestrutura poderiam passar sem validacao, e a cobertura poderia ser confundida com qualidade.

## Decision

Adotar uma piramide de testes com validacao proporcional ao risco:

- **Dominio:** testes unitarios puros, sem contexto Spring.
- **Casos de uso:** testes unitarios com repositorios fake, incluindo replay idempotente e concorrencia local.
- **Web:** testes MockMvc do controller e testes diretos do filtro de resiliencia.
- **Metricas:** testes com `SimpleMeterRegistry` para counters e gauges.
- **Arquitetura:** ArchUnit verifica que o dominio nao depende de infraestrutura nem de Spring.
- **Cobertura:** JaCoCo e executado no ciclo Maven `verify` e gera o relatorio em `target/site/jacoco`.

A cobertura e evidencia de alcance, nao um substituto para cenarios de negocio, testes de falha ou revisao de risco. Nenhum limite minimo bloqueante foi configurado ainda.

## Cenários cobertos

- Criacao, listagem, busca e estorno de lancamentos.
- Validacoes de dominio e erros de negocio.
- Repeticao idempotente sem duplicar lancamento ou outbox.
- Oito chamadas concorrentes do mesmo comando com um unico efeito persistido.
- Rate limit de 30 requisicoes por janela.
- Rejeicao de payload JSON acima de 64 KiB.
- Gauges de total e lancamentos ativos.
- Regras de dependencia da arquitetura hexagonal.

## Alternatives Considered

- Testar somente com `@SpringBootTest`: rejeitado como estrategia unica por ser lento e dificultar diagnostico.
- Usar apenas percentual de cobertura: rejeitado porque nao mede qualidade dos cenarios.
- Mockar o objeto sob teste: rejeitado; mocks ficam nas bordas.
- Usar banco real imediatamente para todos os testes: reservado para uma futura camada de integracao quando a persistencia deixar de ser em memoria.

## Consequences

Positive:

- Falhas de resiliencia e arquitetura ganham regressao automatica.
- A maioria dos testes continua rapida e isolada.
- O relatorio JaCoCo mostra quais classes ainda nao possuem evidencia suficiente.

Negative:

- O rate limiter continua validado apenas localmente, nao em deployment distribuido.
- O smoke test completo do contexto Spring e a integracao com seguranca ainda sao gaps.
- JaCoCo ainda nao falha o build por percentual minimo.

## Validation

Executar a partir de `application`:

```powershell
mvn clean test
mvn clean verify -Djacoco.skip=false
```

Abrir `application/target/site/jacoco/index.html` para revisar a cobertura gerada.
