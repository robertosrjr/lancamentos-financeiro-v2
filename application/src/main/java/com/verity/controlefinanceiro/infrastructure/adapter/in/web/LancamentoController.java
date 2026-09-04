package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import com.verity.controlefinanceiro.application.port.in.ConsultarLancamentosUseCase;
import com.verity.controlefinanceiro.application.port.in.EstornarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lancamentos")
public class LancamentoController {

    private static final Logger logger = LoggerFactory.getLogger(LancamentoController.class);

    private final RegistrarLancamentoUseCase registrarLancamentoUseCase;
    private final ConsultarLancamentosUseCase consultarLancamentosUseCase;
    private final EstornarLancamentoUseCase estornarLancamentoUseCase;

    public LancamentoController(
        RegistrarLancamentoUseCase registrarLancamentoUseCase,
        ConsultarLancamentosUseCase consultarLancamentosUseCase,
        EstornarLancamentoUseCase estornarLancamentoUseCase
    ) {
        this.registrarLancamentoUseCase = registrarLancamentoUseCase;
        this.consultarLancamentosUseCase = consultarLancamentosUseCase;
        this.estornarLancamentoUseCase = estornarLancamentoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse registrar(@Valid @RequestBody LancamentoRequest request, Principal principal) {
        String usuarioId = principal == null ? "anonymous" : principal.getName();

        logger.atInfo()
            .addKeyValue("event", "lancamento.registration.started")
            .addKeyValue("operation", "registrar")
            .log("Lancamento registration started");

        Lancamento lancamento = registrarLancamentoUseCase.registrar(
            new RegistrarLancamentoUseCase.RegistrarLancamentoCommand(
                request.tipo(),
                request.valor(),
                request.data(),
                request.descricao(),
                request.categoria(),
                usuarioId
            )
        );

        logger.atInfo()
            .addKeyValue("event", "lancamento.registration.completed")
            .addKeyValue("operation", "registrar")
            .addKeyValue("lancamentoId", lancamento.id())
            .log("Lancamento registration completed");

        return LancamentoResponse.from(lancamento);
    }

    @GetMapping
    public List<LancamentoResponse> listar() {
        List<Lancamento> lancamentos = consultarLancamentosUseCase.listarTodos();

        logger.atInfo()
            .addKeyValue("event", "lancamento.list.completed")
            .addKeyValue("operation", "listar")
            .addKeyValue("resultCount", lancamentos.size())
            .log("Lancamentos listed");

        return lancamentos.stream()
            .map(LancamentoResponse::from)
            .toList();
    }

    @GetMapping("/{id}")
    public LancamentoResponse buscarPorId(@PathVariable UUID id) {
        Lancamento lancamento = consultarLancamentosUseCase.buscarPorId(id);

        logger.atInfo()
            .addKeyValue("event", "lancamento.lookup.completed")
            .addKeyValue("operation", "buscarPorId")
            .addKeyValue("lancamentoId", id)
            .log("Lancamento found");

        return LancamentoResponse.from(lancamento);
    }

    @PostMapping("/{id}/estorno")
    @ResponseStatus(HttpStatus.CREATED)
    public LancamentoResponse estornar(@PathVariable UUID id) {
        logger.atInfo()
            .addKeyValue("event", "lancamento.reversal.started")
            .addKeyValue("operation", "estornar")
            .addKeyValue("lancamentoId", id)
            .log("Lancamento reversal started");

        Lancamento estorno = estornarLancamentoUseCase.estornar(id);

        logger.atInfo()
            .addKeyValue("event", "lancamento.reversal.completed")
            .addKeyValue("operation", "estornar")
            .addKeyValue("lancamentoId", id)
            .addKeyValue("reversalId", estorno.id())
            .log("Lancamento reversal completed");

        return LancamentoResponse.from(estorno);
    }

    public record LancamentoRequest(
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        String categoria
    ) {}

    public record LancamentoResponse(
        UUID id,
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        String categoria,
        String usuarioId,
        String status,
        UUID lancamentoOrigemId
    ) {
        public static LancamentoResponse from(Lancamento lancamento) {
            return new LancamentoResponse(
                lancamento.id(),
                lancamento.tipo(),
                lancamento.valor().amount(),
                lancamento.data(),
                lancamento.descricao(),
                lancamento.categoria(),
                lancamento.usuarioId(),
                lancamento.status().name(),
                lancamento.lancamentoOrigemId()
            );
        }
    }
}
