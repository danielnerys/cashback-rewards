package com.cashbackrewards.carteira.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Exige o header {@code Idempotency-Key} em endpoints que mudam estado
 * (Principle III / FR-016) e devolve a resposta original quando a mesma
 * chave é reutilizada, em vez de reaplicar o efeito.
 */
@Component
public class IdempotencyInterceptor extends OncePerRequestFilter {

    private static final Set<String> METODOS_MUTAVEIS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final IdempotencyRecordRepository repository;

    public IdempotencyInterceptor(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!METODOS_MUTAVEIS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Header Idempotency-Key é obrigatório");
            return;
        }

        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
        String requestHash = sha256Hex(bodyBytes);

        Optional<IdempotencyRecord> existente = repository.findById(idempotencyKey);
        if (existente.isPresent()) {
            IdempotencyRecord record = existente.get();
            if (!record.getRequestHash().equals(requestHash)) {
                response.sendError(HttpServletResponse.SC_CONFLICT,
                        "Idempotency-Key já usada com um corpo de requisição diferente");
                return;
            }
            response.setStatus(record.getResponseStatus());
            response.setContentType("application/json");
            response.getWriter().write(record.getResponseBody());
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, bodyBytes);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(wrappedRequest, wrappedResponse);

        int status = wrappedResponse.getStatus();
        byte[] responseBytes = wrappedResponse.getContentAsByteArray();
        if (status < 500 && responseBytes.length > 0) {
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            repository.save(new IdempotencyRecord(idempotencyKey, requestHash, responseBody, status,
                    OffsetDateTime.now()));
        }
        wrappedResponse.copyBodyToResponse();
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /**
     * Permite ler o corpo da requisição uma vez (para o hash) e ainda
     * disponibilizá-lo para o restante da cadeia (bind do controller).
     */
    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            InputStream byteArrayInputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    try {
                        return byteArrayInputStream.available() == 0;
                    } catch (IOException e) {
                        return true;
                    }
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // não utilizado em processamento síncrono
                }

                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }
    }
}
