package com.verity.controlefinanceiro.application.port.in;

import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RegistrarLancamentoUseCase {
    Lancamento registrar(RegistrarLancamentoCommand command);

    record RegistrarLancamentoCommand(
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        String categoria,
        String usuarioId
    ) {}
}
