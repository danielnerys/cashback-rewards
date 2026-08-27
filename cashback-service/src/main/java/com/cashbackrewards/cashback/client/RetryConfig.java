package com.cashbackrewards.cashback.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Backoff exponencial com jitter para as chamadas síncronas ao
 * carteira-service, com número máximo de tentativas configurável
 * (constitution — Technology Stack &amp; Service Communication).
 */
@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate carteiraServiceRetryTemplate(
            @Value("${outbox.max-attempts}") int maxAttempts) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
        backOffPolicy.setInitialInterval(200L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(5000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
