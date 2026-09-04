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
