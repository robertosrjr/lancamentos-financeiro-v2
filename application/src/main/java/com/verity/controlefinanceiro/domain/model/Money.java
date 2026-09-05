package com.verity.controlefinanceiro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {
    private static final int ESCALA_PADRAO = 2;

    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (amount.scale() > ESCALA_PADRAO) {
            throw new IllegalArgumentException(
                "Amount must have at most " + ESCALA_PADRAO + " decimal places"
            );
        }
        amount = amount.setScale(ESCALA_PADRAO, RoundingMode.UNNECESSARY);
    }
}
