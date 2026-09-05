package com.verity.controlefinanceiro.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public class Lancamento {
    private final UUID id;
    private final TipoLancamento tipo;
    private final Money valor;
    private final LocalDate data;
    private final String descricao;
    private final String categoria;
    private final String usuarioId;
    private final String idempotencyKey;
    private StatusLancamento status;
    private final UUID lancamentoOrigemId;

    public Lancamento(
        UUID id,
        TipoLancamento tipo,
        Money valor,
        LocalDate data,
        String descricao,
        String categoria
    ) {
        this(id, tipo, valor, data, descricao, categoria, null, StatusLancamento.ATIVO, null, null);
    }

    public Lancamento(
        UUID id,
        TipoLancamento tipo,
        Money valor,
        LocalDate data,
        String descricao,
        String categoria,
        String usuarioId,
        StatusLancamento status,
        UUID lancamentoOrigemId
    ) {
        this(id, tipo, valor, data, descricao, categoria, usuarioId, status, lancamentoOrigemId, null);
    }

    public Lancamento(
        UUID id,
        TipoLancamento tipo,
        Money valor,
        LocalDate data,
        String descricao,
        String categoria,
        String usuarioId,
        StatusLancamento status,
        UUID lancamentoOrigemId,
        String idempotencyKey
    ) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do lançamento é obrigatório");
        }
        if (valor == null) {
            throw new IllegalArgumentException("Valor do lançamento é obrigatório");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data do lançamento é obrigatória");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data do lançamento não pode ser futura");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }

        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
        this.categoria = CategoriaLancamento.normalizar(categoria);
        this.usuarioId = usuarioId;
        this.status = status == null ? StatusLancamento.ATIVO : status;
        this.lancamentoOrigemId = lancamentoOrigemId;
        this.idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
            ? UUID.randomUUID().toString()
            : idempotencyKey;
    }

    public Lancamento estornar() {
        if (this.status == StatusLancamento.ESTORNADO) {
            throw new IllegalStateException("Lançamento já foi estornado");
        }
        if (this.lancamentoOrigemId != null) {
            throw new IllegalStateException(
                "Não é permitido estornar um lançamento que já é um estorno; registre um lançamento de correção"
            );
        }

        TipoLancamento tipoEstorno = switch (this.tipo) {
            case DEBITO -> TipoLancamento.CREDITO;
            case CREDITO -> TipoLancamento.DEBITO;
        };

        this.status = StatusLancamento.ESTORNADO;

        return new Lancamento(
            UUID.randomUUID(),
            tipoEstorno,
            this.valor,
            LocalDate.now(),
            "Estorno de " + this.descricao,
            this.categoria,
            this.usuarioId,
            StatusLancamento.ATIVO,
            this.id,
            null
        );
    }

    public void marcarComoEstornado() {
        this.status = StatusLancamento.ESTORNADO;
    }

    public UUID id() {
        return id;
    }

    public TipoLancamento tipo() {
        return tipo;
    }

    public Money valor() {
        return valor;
    }

    public LocalDate data() {
        return data;
    }

    public String descricao() {
        return descricao;
    }

    public String categoria() {
        return categoria;
    }

    public String usuarioId() {
        return usuarioId;
    }

    public StatusLancamento status() {
        return status;
    }

    public UUID lancamentoOrigemId() {
        return lancamentoOrigemId;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}
