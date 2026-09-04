package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import com.verity.controlefinanceiro.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_handle_business_exception() {
        BusinessException ex = new BusinessException("Regra inválida");

        ProblemDetail result = handler.handleBusinessException(ex);

        assertThat(result.getStatus()).isEqualTo(422);
        assertThat(result.getTitle()).isEqualTo("Regra de negócio inválida");
        assertThat(result.getDetail()).isEqualTo("Regra inválida");
    }

    @Test
    void should_handle_illegal_argument_exception() {
        IllegalArgumentException ex = new IllegalArgumentException("Dados inválidos");

        ProblemDetail result = handler.handleIllegalArgumentException(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getTitle()).isEqualTo("Dados inválidos");
        assertThat(result.getDetail()).isEqualTo("Dados inválidos");
    }

    @Test
    void should_handle_illegal_state_exception() {
        IllegalStateException ex = new IllegalStateException("Conflito de estado");

        ProblemDetail result = handler.handleIllegalStateException(ex);

        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getTitle()).isEqualTo("Conflito de estado");
        assertThat(result.getDetail()).isEqualTo("Conflito de estado");
    }
}
