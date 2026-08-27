package com.cashbackrewards.compras.outbox;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.cashbackrewards.compras.AbstractPostgresIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O OutboxDispatcher chama o cashback-service com uma requisição/resposta
 * síncrona comum (sem 202/accept-and-acknowledge) — este teste substitui o
 * cashback-service real por um stub WireMock para verificar exatamente essa
 * chamada.
 */
@SpringBootTest
class OutboxDispatcherIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final WireMockServer WIRE_MOCK_SERVER =
            new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    static {
        WIRE_MOCK_SERVER.start();
        WireMock.configureFor("localhost", WIRE_MOCK_SERVER.port());
    }

    @AfterAll
    static void pararWireMock() {
        WIRE_MOCK_SERVER.stop();
    }

    @DynamicPropertySource
    static void cashbackServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("cashback-service.base-url", () -> "http://localhost:" + WIRE_MOCK_SERVER.port());
    }

    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Test
    void despachar_chamaCreditosDoCashbackServiceEMarcaEnviado() {
        String idempotencyKey = "outbox-" + UUID.randomUUID();
        stubFor(post(urlEqualTo("/v1/creditos")).willReturn(aResponse().withStatus(200)));

        OutboxEntry entrada = new OutboxEntry(UUID.randomUUID(), OutboxEntry.TipoEvento.COMPRA_REGISTRADA,
                """
                {"compraId":"%s","usuarioId":"%s","valorCentavos":10000,"categoria":"geral"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()),
                idempotencyKey);
        outboxRepository.save(entrada);

        outboxDispatcher.despachar();

        verify(postRequestedFor(urlEqualTo("/v1/creditos"))
                .withHeader("Idempotency-Key", equalTo(idempotencyKey)));

        OutboxEntry atualizada = outboxRepository.findById(entrada.getId()).orElseThrow();
        assertThat(atualizada.getEstado()).isEqualTo(OutboxEntry.Estado.ENVIADO);
    }
}
