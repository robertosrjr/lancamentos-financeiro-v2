package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import com.verity.controlefinanceiro.application.port.in.ConsultarLancamentosUseCase;
import com.verity.controlefinanceiro.application.port.in.EstornarLancamentoUseCase;
import com.verity.controlefinanceiro.application.port.in.RegistrarLancamentoUseCase;
import com.verity.controlefinanceiro.domain.model.Lancamento;
import com.verity.controlefinanceiro.domain.model.Money;
import com.verity.controlefinanceiro.domain.model.StatusLancamento;
import com.verity.controlefinanceiro.domain.model.TipoLancamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LancamentoControllerTest {

    private MockMvc mockMvc;
    private RegistrarLancamentoUseCase registrarLancamentoUseCase;
    private ConsultarLancamentosUseCase consultarLancamentosUseCase;
    private EstornarLancamentoUseCase estornarLancamentoUseCase;

    @BeforeEach
    void setUp() {
        registrarLancamentoUseCase = mock(RegistrarLancamentoUseCase.class);
        consultarLancamentosUseCase = mock(ConsultarLancamentosUseCase.class);
        estornarLancamentoUseCase = mock(EstornarLancamentoUseCase.class);

        mockMvc = MockMvcBuilders.standaloneSetup(
            new LancamentoController(
                registrarLancamentoUseCase,
                consultarLancamentosUseCase,
                estornarLancamentoUseCase
            )
        ).build();
    }

    @Test
    void should_create_lancamento() throws Exception {
        UUID id = UUID.randomUUID();
        Lancamento lancamento = new Lancamento(
            id,
            TipoLancamento.CREDITO,
            new Money(new BigDecimal("150.00"), java.util.Currency.getInstance("BRL")),
            LocalDate.now(),
            "Venda",
            "Vendas"
        );

        given(registrarLancamentoUseCase.registrar(any())).willReturn(lancamento);

        mockMvc.perform(post("/api/v1/lancamentos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"CREDITO\",\"valor\":150.00,\"data\":\"" + LocalDate.now() + "\",\"descricao\":\"Venda\",\"categoria\":\"Vendas\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("CREDITO"))
            .andExpect(jsonPath("$.descricao").value("Venda"));
    }

    @Test
    void should_list_lancamentos() throws Exception {
        UUID id = UUID.randomUUID();
        Lancamento lancamento = new Lancamento(
            id,
            TipoLancamento.DEBITO,
            new Money(new BigDecimal("80.00"), java.util.Currency.getInstance("BRL")),
            LocalDate.now(),
            "Compra",
            "Compras"
        );

        given(consultarLancamentosUseCase.listarTodos()).willReturn(List.of(lancamento));

        mockMvc.perform(get("/api/v1/lancamentos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].status").value(StatusLancamento.ATIVO.name()));
    }
}
