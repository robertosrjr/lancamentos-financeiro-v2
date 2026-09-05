package com.verity.controlefinanceiro.domain.model;

import java.text.Normalizer;
import java.util.Locale;

public enum CategoriaLancamento {
    RECEITAS("Receitas"),
    VENDAS("Vendas"),
    DESPESAS("Despesas"),
    COMPRAS("Compras"),
    SERVICOS("Serviços"),
    IMPOSTOS("Impostos"),
    OUTROS("Outros");

    private final String rotulo;

    CategoriaLancamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String chave = chaveComparavel(valor);
        for (CategoriaLancamento categoria : values()) {
            if (chaveComparavel(categoria.name()).equals(chave)) {
                return categoria.rotulo();
            }
        }

        throw new IllegalArgumentException(
            "Categoria não pertence ao catálogo permitido: " + valor.trim()
        );
    }

    private static String chaveComparavel(String valor) {
        String semAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return semAcentos.toUpperCase(Locale.ROOT);
    }
}
