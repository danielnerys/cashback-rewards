package com.cashbackrewards.compras.client;

import com.cashbackrewards.compras.api.CorrelationIdFilter;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/**
 * Propaga o {@code X-Correlation-Id} corrente (Principle V) em toda chamada
 * de saída para outro serviço, incluindo as disparadas pelo job do outbox.
 */
@Component
public class CorrelationIdPropagatingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            request.getHeaders().add(CorrelationIdFilter.HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
