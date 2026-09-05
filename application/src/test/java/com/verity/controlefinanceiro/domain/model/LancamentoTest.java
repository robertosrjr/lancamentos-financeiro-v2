package com.verity.controlefinanceiro.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LancamentoTest {

    @Test
    void should_create_money_with_positive_amount() {
        Money money = new Money(new BigDecimal("150.00"), Currency.getInstance("BRL"));

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(money.currency()).isEqualTo(Currency.getInstance("BRL"));
    }

    @Test
    void should_reject_negative_amount_when_creating_money() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-10.00"), Currency.getInstance("BRL")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void should_reject_zero_amount_when_creating_money() {
        assertThatThrownBy(() -> new Money(BigDecimal.ZERO, Currency.getInstance("BRL")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void should_reject_null_amount_when_creating_money() {
        assertThatThrownBy(() -> new Money(null, Currency.getInstance("BRL")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void should_reject_null_currency_when_creating_money() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.00"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Currency");
    }

    @Test
    void should_reject_lancamento_with_null_type() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            null,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra",
            "Compras"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tipo");
    }

    @Test
    void should_reject_lancamento_with_null_value() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            null,
            LocalDate.now(),
            "Compra",
            "Compras"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Valor");
    }

    @Test
    void should_reject_lancamento_with_null_date() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            null,
            "Compra",
            "Compras"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Data");
    }

    @Test
    void should_reject_lancamento_with_future_date() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now().plusDays(1),
            "Compra",
            "Compras"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("futura");
    }

    @Test
    void should_reject_lancamento_with_blank_description() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "   ",
            "Compras"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Descrição");
    }

    @Test
    void should_register_despesa_without_categoria() {
        Lancamento despesa = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("45.90"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra sem categoria definida",
            null
        );

        assertThat(despesa.categoria()).isNull();
        assertThat(despesa.status()).isEqualTo(StatusLancamento.ATIVO);
    }

    @Test
    void should_accept_lancamento_dated_today() {
        Lancamento lancamento = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Recebimento do dia",
            "Vendas"
        );

        assertThat(lancamento.data()).isEqualTo(LocalDate.now());
    }

    @Test
    void should_invert_type_to_credito_when_estornando_despesa() {
        Lancamento despesa = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("60.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra de material",
            "Compras"
        );

        Lancamento estorno = despesa.estornar();

        assertThat(estorno.tipo()).isEqualTo(TipoLancamento.CREDITO);
        assertThat(estorno.valor().amount()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void should_date_estorno_with_current_date_even_when_original_is_old() {
        Lancamento antigo = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("300.00"), Currency.getInstance("BRL")),
            LocalDate.now().minusMonths(6),
            "Receita antiga",
            "Vendas"
        );

        Lancamento estorno = antigo.estornar();

        assertThat(estorno.data()).isEqualTo(LocalDate.now());
        assertThat(antigo.data()).isEqualTo(LocalDate.now().minusMonths(6));
    }

    @Test
    void should_reject_estorno_of_an_estorno_lancamento() {
        Lancamento original = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("90.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Lançamento indevido",
            "Compras"
        );

        Lancamento estorno = original.estornar();

        assertThatThrownBy(estorno::estornar)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("estorno");
    }

    @Test
    void should_reject_money_with_more_than_two_decimal_places() {
        assertThatThrownBy(() -> new Money(new BigDecimal("10.999"), Currency.getInstance("BRL")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("decimal");
    }

    @Test
    void should_normalize_money_scale_to_two_decimal_places() {
        Money semCasasDecimais = new Money(new BigDecimal("10"), Currency.getInstance("BRL"));
        Money umaCasaDecimal = new Money(new BigDecimal("10.5"), Currency.getInstance("BRL"));

        assertThat(semCasasDecimais.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(semCasasDecimais.amount().scale()).isEqualTo(2);
        assertThat(umaCasaDecimal.amount()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(umaCasaDecimal.amount().scale()).isEqualTo(2);
    }

    @Test
    void should_reject_categoria_outside_allowed_catalog() {
        assertThatThrownBy(() -> new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra",
            "Categoria Inexistente"
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("catálogo");
    }

    @Test
    void should_normalize_categoria_case_and_whitespace_to_catalog_label() {
        Lancamento lancamento = new Lancamento(
            UUID.randomUUID(),
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("10.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra",
            " compras "
        );

        assertThat(lancamento.categoria()).isEqualTo("Compras");
    }

    @Test
    void should_create_estorno_with_positive_amount_and_inverted_type() {
        UUID originalId = UUID.randomUUID();
        Lancamento original = new Lancamento(
            originalId,
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("100.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Venda",
            "Vendas"
        );

        Lancamento estorno = original.estornar();

        assertThat(estorno.valor().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(estorno.tipo()).isEqualTo(TipoLancamento.DEBITO);
        assertThat(estorno.lancamentoOrigemId()).isEqualTo(originalId);
        assertThat(estorno.status()).isEqualTo(StatusLancamento.ATIVO);
    }

    @Test
    void should_reject_estorno_for_already_cancelled_lancamento() {
        UUID originalId = UUID.randomUUID();
        Lancamento original = new Lancamento(
            originalId,
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("80.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra",
            "Compras"
        );

        original.marcarComoEstornado();

        assertThatThrownBy(original::estornar)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("estornado");
    }

    @Test
    void should_mark_original_lancamento_as_estornado_when_creating_reversal() {
        UUID originalId = UUID.randomUUID();
        Lancamento original = new Lancamento(
            originalId,
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("250.00"), Currency.getInstance("BRL")),
            LocalDate.now(),
            "Receita",
            "Vendas"
        );

        Lancamento estorno = original.estornar();

        assertThat(original.status()).isEqualTo(StatusLancamento.ESTORNADO);
        assertThat(estorno.lancamentoOrigemId()).isEqualTo(originalId);
        assertThat(estorno.tipo()).isEqualTo(TipoLancamento.DEBITO);
    }
}
