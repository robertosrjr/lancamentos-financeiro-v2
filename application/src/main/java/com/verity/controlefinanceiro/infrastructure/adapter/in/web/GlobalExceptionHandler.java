package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import com.verity.controlefinanceiro.domain.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        logRejectedRequest("business_rule", HttpStatus.UNPROCESSABLE_ENTITY);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Regra de negócio inválida");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        logRejectedRequest("invalid_argument", HttpStatus.BAD_REQUEST);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Dados inválidos");
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        logRejectedRequest("invalid_state", HttpStatus.CONFLICT);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de estado");
        return problem;
    }

    private void logRejectedRequest(String errorType, HttpStatus status) {
        logger.atWarn()
            .addKeyValue("event", "http.request.rejected")
            .addKeyValue("errorType", errorType)
            .addKeyValue("status", status.value())
            .log("HTTP request rejected");
    }
}
