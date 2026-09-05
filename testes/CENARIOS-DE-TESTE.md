# Cenarios de Teste de Negocio - Controle Financeiro

Este documento cataloga os cenarios de teste com foco em regra de negocio do dominio de Controle Financeiro, complementando a cobertura tecnica descrita na [ADR-0006](adrs/0006-test-strategy-and-coverage.md). As regras de escala monetaria, catalogo de categorias e limite de estorno estao registradas na [ADR-0007](adrs/0007-business-rules-estorno-escala-categoria.md).

Cada cenario aponta a classe e o metodo de teste correspondente para rastreabilidade. Todos os cenarios abaixo estao implementados e passam na suite atual (`mvn test`).

## 1. Registro de Lancamento

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 1.1 | Dado um lancamento de despesa (DEBITO) valido, quando registrado, entao os dados sao persistidos com status ATIVO e chave de idempotencia gerada. | `RegistrarLancamentoUseCaseImplTest.should_register_lancamento_with_brl_currency_and_saved_values` |
| 1.2 | Dado um comando de registro, quando processado, entao a chave de idempotencia e um SHA-256 estavel e recuperavel pelo outbox. | `RegistrarLancamentoUseCaseImplTest.should_generate_stable_idempotency_key_for_lancamento_event` |
| 1.3 | Dado um lancamento sem categoria informada, quando criado, entao a categoria fica nula e o status e ATIVO (categoria e opcional, descricao nao). | `LancamentoTest.should_register_despesa_without_categoria` |
| 1.4 | Dado um lancamento datado com o dia corrente, quando criado, entao e aceito (o limite de negocio e "nao pode ser no futuro", hoje e a fronteira). | `LancamentoTest.should_accept_lancamento_dated_today` |
| 1.5 | Dado um reenvio identico do mesmo comando (double-submit), quando registrado novamente, entao o lancamento original e retornado sem duplicar (idempotencia). | `RegistrarLancamentoUseCaseImplTest.should_return_existing_lancamento_when_registration_is_replayed` |
| 1.6 | Dado o mesmo payload enviado por dois usuarios diferentes, quando registrados, entao geram dois lancamentos distintos (a chave de idempotencia e por usuario). | `RegistrarLancamentoUseCaseImplTest.should_create_distinct_lancamentos_for_different_users_with_same_payload` |
| 1.7 | Dado um usuario que corrige um campo (ex.: valor) e reenvia, quando registrado, entao e tratado como um novo lancamento, nao como replay. | `RegistrarLancamentoUseCaseImplTest.should_treat_corrected_value_as_new_registration_instead_of_replay` |
| 1.8 | Dado o mesmo comando enviado por 8 threads simultaneas, quando processado, entao apenas um lancamento e um evento de outbox sao criados. | `RegistrarLancamentoUseCaseImplTest.should_create_only_one_lancamento_when_same_command_is_registered_concurrently` |
| 1.9 | Dado tipo, valor, data ou descricao ausentes/invalidos, quando criado o lancamento, entao a criacao e rejeitada com mensagem especifica do campo. | `LancamentoTest.should_reject_lancamento_with_null_type` / `_null_value` / `_null_date` / `_future_date` / `_blank_description` |

## 2. Estorno de Lancamento

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 2.1 | Dado um lancamento de despesa (DEBITO) ativo, quando estornado, entao gera um lancamento de credito de mesmo valor (devolucao). | `LancamentoTest.should_invert_type_to_credito_when_estornando_despesa` |
| 2.2 | Dado um lancamento de receita (CREDITO) ativo, quando estornado, entao gera um lancamento de debito de mesmo valor (estorno de venda). | `LancamentoTest.should_create_estorno_with_positive_amount_and_inverted_type` |
| 2.3 | Dado um lancamento antigo, quando estornado, entao o estorno recebe a data corrente, nao a data original. | `LancamentoTest.should_date_estorno_with_current_date_even_when_original_is_old` |
| 2.4 | Dado um lancamento ja estornado, quando um novo estorno e solicitado, entao a operacao e rejeitada (`IllegalStateException`, "ja foi estornado"). | `LancamentoTest.should_reject_estorno_for_already_cancelled_lancamento`, `EstornarLancamentoUseCaseImplTest.should_throw_when_lancamento_already_estornado` |
| 2.5 | Dado um lancamento que ja e, ele proprio, um estorno, quando se tenta estorna-lo novamente, entao a operacao e rejeitada (limite de 1 nivel de reversao, ver ADR-0007). | `LancamentoTest.should_reject_estorno_of_an_estorno_lancamento`, `EstornarLancamentoUseCaseImplTest.should_reject_estorno_of_an_estorno_lancamento` |
| 2.6 | Dado um lancamento estornado, quando o estorno e criado, entao o original fica marcado ESTORNADO e o novo lancamento referencia a origem (`lancamentoOrigemId`). | `LancamentoTest.should_mark_original_lancamento_as_estornado_when_creating_reversal` |
| 2.7 | Dado um identificador de lancamento inexistente, quando o estorno e solicitado, entao a operacao e rejeitada com erro de negocio claro. | `EstornarLancamentoUseCaseImplTest.should_throw_when_lancamento_not_found` |

## 3. Consulta de Lancamentos

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 3.1 | Dado um lancamento existente, quando buscado por ID, entao os dados retornados sao os corretos. | `ConsultarLancamentosUseCaseImplTest.should_find_existing_lancamento_by_id` |
| 3.2 | Dado um ID inexistente, quando buscado, entao a operacao e rejeitada com erro de negocio claro. | `ConsultarLancamentosUseCaseImplTest.should_throw_when_lancamento_does_not_exist` |
| 3.3 | Dada uma conta sem nenhum lancamento, quando listada, entao retorna uma lista vazia. | `ConsultarLancamentosUseCaseImplTest.should_return_empty_list_when_no_lancamentos_exist` |
| 3.4 | Dados lancamentos ativos e estornados, quando listados, entao ambos aparecem juntos (historico completo de auditoria, nada e ocultado). | `ConsultarLancamentosUseCaseImplTest.should_list_both_active_and_estornado_lancamentos_for_full_audit_history` |

## 4. Regras Monetarias (Money)

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 4.1 | Dado um valor nulo, zero ou negativo, quando criado o `Money`, entao a criacao e rejeitada (lancamento sem efeito financeiro nao existe). | `LancamentoTest.should_reject_negative_amount_when_creating_money` / `_zero_amount_` / `_null_amount_` |
| 4.2 | Dado um valor com mais de 2 casas decimais (ex.: `10.999`), quando criado o `Money`, entao a criacao e rejeitada explicitamente (nunca truncada em silencio). | `LancamentoTest.should_reject_money_with_more_than_two_decimal_places`, `RegistrarLancamentoUseCaseImplTest.should_reject_registration_when_value_has_more_than_two_decimal_places` |
| 4.3 | Dado um valor com 0 ou 1 casa decimal (ex.: `10`, `10.5`), quando criado o `Money`, entao e normalizado para escala 2 (`10.00`, `10.50`). | `LancamentoTest.should_normalize_money_scale_to_two_decimal_places` |
| 4.4 | Dado o mesmo valor monetario escrito com escalas diferentes (`10.5` vs `10.50`) pelo mesmo usuario, quando registrado dus vezes, entao e reconhecido como o mesmo lancamento (idempotencia nao quebra por escala). | `RegistrarLancamentoUseCaseImplTest.should_treat_different_decimal_scale_as_same_value_for_idempotency` |
| 4.5 | Dado um `Currency` nulo, quando criado o `Money`, entao a criacao e rejeitada. | `LancamentoTest.should_reject_null_currency_when_creating_money` |

## 5. Catalogo de Categorias

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 5.1 | Dada uma categoria fora do catalogo permitido (ex.: "Categoria Inexistente"), quando o lancamento e criado, entao a criacao e rejeitada. | `LancamentoTest.should_reject_categoria_outside_allowed_catalog`, `RegistrarLancamentoUseCaseImplTest.should_reject_registration_with_category_outside_catalog` |
| 5.2 | Dada uma categoria valida escrita com variacao de caixa/espacos (ex.: `" compras "`), quando o lancamento e criado, entao e normalizada para o rotulo canonico do catalogo (`"Compras"`). | `LancamentoTest.should_normalize_categoria_case_and_whitespace_to_catalog_label` |
| 5.3 | Dado o mesmo lancamento registrado com categoria em caixas diferentes (`"compras"` vs `"Compras"`) pelo mesmo usuario, quando registrado duas vezes, entao e reconhecido como o mesmo lancamento (idempotencia nao quebra por caixa da categoria). | `RegistrarLancamentoUseCaseImplTest.should_treat_different_category_casing_as_same_value_for_idempotency` |

## 6. Idempotencia e Auditoria (Outbox)

| # | Cenario (Dado / Quando / Entao) | Teste |
| --- | --- | --- |
| 6.1 | Dado um registro bem-sucedido, quando concluido, entao um evento de outbox e salvo com a mesma chave de idempotencia do lancamento. | `RegistrarLancamentoUseCaseImplTest.should_register_lancamento_with_brl_currency_and_saved_values` |
| 6.2 | Dado um reenvio do mesmo comando, quando reprocessado, entao nem o lancamento nem o evento de outbox sao duplicados. | `RegistrarLancamentoUseCaseImplTest.should_return_existing_lancamento_when_registration_is_replayed` |
| 6.3 | Dada uma falha ao salvar o evento de outbox, quando o registro e processado, entao o lancamento ja fica persistido mesmo sem o evento de auditoria (risco documentado, nao corrigido nesta rodada). | `RegistrarLancamentoUseCaseImplTest.should_persist_lancamento_even_when_outbox_event_save_fails` |

## Riscos conhecidos sem correcao aplicada

- **Concorrencia no estorno**: `EstornarLancamentoUseCaseImpl.estornar()` nao e sincronizado por `lancamentoId` (diferente de `RegistrarLancamentoUseCaseImpl`, que e `synchronized`). Duas chamadas concorrentes de estorno sobre o mesmo lancamento podem, em tese, duplicar a reversao. Nao ha teste de concorrencia para este caso ainda; registrado como item 12 em "Limitacoes e proximos passos" do `README.md`.
- **Inconsistencia lancamento/outbox em falha** (cenario 6.3): o lancamento fica persistido mesmo quando o evento de auditoria falha ao salvar. O comportamento esta documentado e coberto por teste, mas nao ha compensacao/rollback implementado.
