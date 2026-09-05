# ADR-0007: Governanca de Regras de Negocio - Estorno, Escala Monetaria e Categorias

- Status: Accepted
- Date: 2026-09-05

## Context

Uma analise de Arquitetura de Negocio (alinhada ao TAGF) sobre o dominio de Controle Financeiro identificou tres lacunas de regra de negocio sem decisao formal registrada:

1. Estorno de um lancamento que ja e, ele proprio, um estorno era permitido sem limite (cadeia de reversoes).
2. O valor monetario nao tinha escala decimal normalizada, o que tambem quebrava a idempotencia: `10.5` e `10.50` geravam chaves diferentes para o mesmo valor.
3. A categoria era texto livre, permitindo que `"Compras"`, `"compras"` e `" Compras "` fossem tratadas como categorias distintas nos relatorios.

## Decision

1. **Estorno limitado a um unico nivel**: `Lancamento.estornar()` e `EstornarLancamentoUseCaseImpl` rejeitam a reversao de um lancamento que ja possui `lancamentoOrigemId` preenchido (ou seja, que ja e ele proprio um estorno), lancando `IllegalStateException`. Corrigir um estorno indevido exige um novo lancamento de ajuste, nao uma cadeia de reversoes.
2. **Escala monetaria fixa em 2 casas decimais**: `Money` rejeita valores com mais de 2 casas decimais (`IllegalArgumentException`) e normaliza o valor armazenado para escala 2 (`10` vira `10.00`, `10.5` vira `10.50`). O hash de idempotencia em `RegistrarLancamentoUseCaseImpl` passa a usar o valor ja normalizado (`money.amount()`), eliminando a divergencia de chave para o mesmo valor monetario escrito com escalas diferentes.
3. **Catalogo fechado de categorias**: `CategoriaLancamento` define o catalogo permitido (Receitas, Vendas, Despesas, Compras, Servicos, Impostos, Outros). `Lancamento` normaliza a categoria informada (trim, comparacao sem distincao de maiusculas/acentos) para o rotulo canonico do catalogo, ou rejeita com `IllegalArgumentException` quando o valor esta fora da lista. O hash de idempotencia tambem usa a categoria ja normalizada.

## Alternatives Considered

- Permitir estorno em cadeia sem limite: rejeitado por dificultar a trilha de auditoria e abrir espaco para acumulo indevido de reversoes sobre o mesmo lancamento original.
- Truncar ou arredondar silenciosamente valores com mais de 2 casas decimais: rejeitado porque pode gerar disputa de auditoria sobre perda de centavos; rejeicao explicita e mais segura em um dominio financeiro.
- Manter categoria como texto livre com apenas normalizacao de caixa/espacos: rejeitado porque nao impede a proliferacao de categorias nao governadas; o catalogo fechado sustenta relatorios consistentes.
- Workflow completo de governanca de taxonomia (proposta -> aprovacao -> publicacao -> depreciacao) com um data steward: considerado, mas adiado por ser desproporcional ao estagio atual do projeto. O catalogo fechado simples cobre a necessidade imediata.

## Consequences

Positive:

- Elimina por construcao a divergencia de chave de idempotencia causada por escala decimal ou variacao de caixa/espacos na categoria para o mesmo lancamento.
- Reduz o risco de manipulacao de saldo por cadeias de estorno.
- Relatorios por categoria deixam de fragmentar o mesmo grupo de negocio em variantes de texto.

Negative:

- Corrigir um estorno indevido agora exige um lancamento de ajuste manual em vez de um segundo estorno automatico.
- O catalogo de categorias e fixo no codigo; adicionar uma categoria nova exige alteracao e novo deploy, ainda sem mecanismo de curadoria em runtime.
- Valores historicos eventualmente gravados com escala diferente antes desta mudanca nao sao migrados retroativamente por esta decisao.

## Future Work

- Elevar o catalogo de categorias para um dado de referencia administravel por um responsavel de negocio (data steward), sem exigir deploy para cadastrar ou depreciar categorias.
- Definir formalmente a alcada de aprovacao para o lancamento de ajuste que substitui o estorno em cadeia.
- Se o dominio evoluir para multi-moeda, tornar a escala decimal dependente do codigo ISO 4217 da moeda em vez de fixa em 2 casas.
