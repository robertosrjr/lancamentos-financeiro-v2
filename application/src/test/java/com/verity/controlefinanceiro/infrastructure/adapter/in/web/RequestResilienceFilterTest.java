package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResilienceFilterTest {

    private SimpleMeterRegistry meterRegistry;
    private RequestResilienceFilter filter;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new RequestResilienceFilter(meterRegistry);
    }

    @Test
    void should_allow_write_requests_until_rate_limit_is_reached() throws Exception {
        for (int attempt = 1; attempt <= 30; attempt++) {
            MockHttpServletResponse response = doFilter("POST", "/api/v1/lancamentos", "127.0.0.1", "{}");

            assertThat(response.getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rejected = doFilter("POST", "/api/v1/lancamentos", "127.0.0.1", "{}");

        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(meterRegistry.get("app.http.rate_limited").counter().count()).isEqualTo(1.0);
    }

    @Test
    void should_reject_json_payload_larger_than_limit() throws Exception {
        String payload = "x".repeat(64 * 1024 + 1);

        MockHttpServletResponse response = doFilter(
            "POST", "/api/v1/lancamentos", "127.0.0.2", payload
        );

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(meterRegistry.get("app.http.payload_rejected").counter().count()).isEqualTo(1.0);
    }

    @Test
    void should_not_rate_limit_read_requests() throws Exception {
        MockHttpServletResponse response = doFilter("GET", "/api/v1/lancamentos", "127.0.0.1", "");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(meterRegistry.get("app.http.rate_limited").counter().count()).isZero();
    }

    private MockHttpServletResponse doFilter(
        String method,
        String uri,
        String remoteAddress,
        String body
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setRemoteAddr(remoteAddress);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
