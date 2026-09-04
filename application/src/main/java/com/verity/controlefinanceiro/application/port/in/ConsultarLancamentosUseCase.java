package com.verity.controlefinanceiro.application.port.in;

import com.verity.controlefinanceiro.domain.model.Lancamento;

import java.util.List;
import java.util.UUID;

public interface ConsultarLancamentosUseCase {
    List<Lancamento> listarTodos();
    Lancamento buscarPorId(UUID id);
}
