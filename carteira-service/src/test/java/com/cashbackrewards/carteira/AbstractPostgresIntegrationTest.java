package com.cashbackrewards.carteira;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de integração/contrato: PostgreSQL real via Testcontainers
 * (constitution — Development Workflow &amp; Quality Gates), nunca banco
 * embutido/in-memory.
 *
 * <p>Padrão "singleton container": o container é iniciado uma única vez para
 * toda a JVM de teste e nunca é parado explicitamente (o Ryuk do
 * Testcontainers o remove ao final da JVM). Ver a mesma classe em
 * compras-service para a explicação completa do porquê (cache de contexto do
 * Spring Test pode reaproveitar um ApplicationContext cujo DataSource aponta
 * para um container já parado de uma classe anterior).
 */
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
