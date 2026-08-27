package com.cashbackrewards.cashback.outbox;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.cashbackrewards.cashback.AbstractPostgresIntegrationTest;
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

/**
 * O OutboxDispatcher chama o carteira-service com uma requisição/resposta
 * síncrona comum, no caminho `/v1/usuarios/{usuarioId}/creditos`, extraindo
 * o usuarioId do payload persistido (research.md #1).
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
    static void carteiraServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("carteira-service.base-url", () -> "http://localhost:" + WIRE_MOCK_SERVER.port());
    }

    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Test
    void despachar_chamaCreditosDoCarteiraServiceNoCaminhoDoUsuarioEMarcaEnviado() {
        String usuarioId = UUID.randomUUID().toString();
        String idempotencyKey = "outbox-" + UUID.randomUUID();
        String caminhoEsperado = "/v1/usuarios/" + usuarioId + "/creditos";

        stubFor(post(urlEqualTo(caminhoEsperado)).willReturn(aResponse().withStatus(201)));

        OutboxEntry entrada = new OutboxEntry(UUID.randomUUID(), OutboxEntry.TipoEvento.CREDITO_CALCULADO,
                """
                {"usuarioId":"%s","creditoId":"%s","valorCentavos":500}
                """.formatted(usuarioId, UUID.randomUUID()),
                idempotencyKey);
        outboxRepository.save(entrada);

        outboxDispatcher.despachar();

        verify(postRequestedFor(urlEqualTo(caminhoEsperado))
                .withHeader("Idempotency-Key", equalTo(idempotencyKey)));

        OutboxEntry atualizada = outboxRepository.findById(entrada.getId()).orElseThrow();
        assertThat(atualizada.getEstado()).isEqualTo(OutboxEntry.Estado.ENVIADO);
    }
}
