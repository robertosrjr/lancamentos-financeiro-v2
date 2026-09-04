package com.verity.controlefinanceiro.application.port.in;

import com.verity.controlefinanceiro.domain.model.Lancamento;

import java.util.UUID;

public interface EstornarLancamentoUseCase {
    Lancamento estornar(UUID lancamentoId);
}
