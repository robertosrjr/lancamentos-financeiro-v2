package com.verity.controlefinanceiro.infrastructure.adapter.in.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestResilienceFilter extends OncePerRequestFilter {

    private static final int MAX_JSON_BYTES = 64 * 1024;
    private static final int REQUESTS_PER_WINDOW = 30;
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);

    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();
    private final Counter rateLimitedRequests;
    private final Counter oversizedRequests;

    public RequestResilienceFilter(MeterRegistry meterRegistry) {
        this.rateLimitedRequests = Counter.builder("app.http.rate_limited")
            .description("HTTP requests rejected by the application rate limiter")
            .register(meterRegistry);
        this.oversizedRequests = Counter.builder("app.http.payload_rejected")
            .description("HTTP requests rejected because the JSON payload was too large")
            .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isWriteRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allow(requestKey(request))) {
            rateLimitedRequests.increment();
            response.sendError(429, "Limite de requisições excedido");
            return;
        }

        if (isJson(request) && request.getContentLengthLong() > MAX_JSON_BYTES) {
            oversizedRequests.increment();
            response.sendError(413, "Payload JSON excede o limite permitido");
            return;
        }

        if (isJson(request)) {
            filterChain.doFilter(new SizeLimitedRequest(request, MAX_JSON_BYTES), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isWriteRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
            && request.getRequestURI().startsWith("/api/v1/lancamentos");
    }

    private boolean isJson(HttpServletRequest request) {
        return request.getContentType() != null
            && request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private String requestKey(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private boolean allow(String clientKey) {
        long now = System.currentTimeMillis();
        if (clients.size() >= MAX_TRACKED_CLIENTS) {
            clients.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= RATE_WINDOW.toMillis());
        }
        ClientWindow window = clients.compute(clientKey, (key, current) -> {
            if (current == null || now - current.startedAt >= RATE_WINDOW.toMillis()) {
                return new ClientWindow(now, new AtomicInteger(1));
            }
            current.count.incrementAndGet();
            return current;
        });
        return window.count.get() <= REQUESTS_PER_WINDOW;
    }

    private static final class ClientWindow {
        private final long startedAt;
        private final AtomicInteger count;

        private ClientWindow(long startedAt, AtomicInteger count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }

    private static final class SizeLimitedRequest extends HttpServletRequestWrapper {
        private final int maxBytes;

        private SizeLimitedRequest(HttpServletRequest request, int maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedServletInputStream(super.getInputStream(), maxBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(
                getInputStream(), encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)
            ));
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final int maxBytes;
        private int bytesRead;

        private LimitedServletInputStream(ServletInputStream delegate, int maxBytes) {
            this.delegate = delegate;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0 && ++bytesRead > maxBytes) {
                throw new PayloadTooLargeException();
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int bytes = delegate.read(buffer, offset, length);
            if (bytes > 0 && (bytesRead += bytes) > maxBytes) {
                throw new PayloadTooLargeException();
            }
            return bytes;
        }

        @Override
        public boolean isFinished() { return delegate.isFinished(); }

        @Override
        public boolean isReady() { return delegate.isReady(); }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) { delegate.setReadListener(listener); }
    }

    private static final class PayloadTooLargeException extends IOException {
        private PayloadTooLargeException() {
            super("Payload JSON excede o limite permitido");
        }
    }
}