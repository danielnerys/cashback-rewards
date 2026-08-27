package com.cashbackrewards.carteira.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cashbackrewards.carteira.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato de POST /v1/usuarios/{usuarioId}/creditos
 * (contracts/carteira-service.yaml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AplicarCreditoContractTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aplicarCredito_retorna201EAtualizaSaldo() throws Exception {
        String usuarioId = UUID.randomUUID().toString();

        mockMvc.perform(post("/v1/usuarios/{usuarioId}/creditos", usuarioId)
                        .header("Idempotency-Key", "contract-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"creditoId":"%s","valorCentavos":500}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CREDITO"))
                .andExpect(jsonPath("$.valorCentavos").value(500))
                .andExpect(jsonPath("$.saldoApos").value(500));
    }
}
