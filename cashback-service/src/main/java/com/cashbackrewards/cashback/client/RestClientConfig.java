package com.cashbackrewards.cashback.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP síncrono direto para o carteira-service (research.md #2).
 * Timeout de conexão/leitura explícitos; nenhuma chamada fica bloqueada
 * indefinidamente (constitution — Technology Stack &amp; Service Communication).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient carteiraServiceRestClient(
            @Value("${carteira-service.base-url}") String baseUrl,
            @Value("${carteira-service.connect-timeout-ms}") long connectTimeoutMs,
            @Value("${carteira-service.read-timeout-ms}") long readTimeoutMs,
            CorrelationIdPropagatingInterceptor correlationIdPropagatingInterceptor) {

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(correlationIdPropagatingInterceptor)
                .build();
    }
}
