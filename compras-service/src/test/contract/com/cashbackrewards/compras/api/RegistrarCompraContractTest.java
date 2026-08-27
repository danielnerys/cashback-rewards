package com.cashbackrewards.compras.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cashbackrewards.compras.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato de POST /v1/compras (contracts/compras-service.yaml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RegistrarCompraContractTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrarCompra_comCorpoValido_retorna201ComCompraAtiva() throws Exception {
        String usuarioId = UUID.randomUUID().toString();
        mockMvc.perform(post("/v1/compras")
                        .header("Idempotency-Key", "contract-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":"%s","valorCentavos":10000,"categoria":"restaurantes","data":"2026-08-27T12:00:00Z"}
                                """.formatted(usuarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.usuarioId").value(usuarioId))
                .andExpect(jsonPath("$.valorCentavos").value(10000))
                .andExpect(jsonPath("$.categoria").value("restaurantes"))
                .andExpect(jsonPath("$.estado").value("ATIVA"));
    }

    @Test
    void registrarCompra_semIdempotencyKey_retorna400() throws Exception {
        mockMvc.perform(post("/v1/compras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":"%s","valorCentavos":10000,"categoria":"restaurantes","data":"2026-08-27T12:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarCompra_comValorZero_retorna400() throws Exception {
        mockMvc.perform(post("/v1/compras")
                        .header("Idempotency-Key", "contract-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":"%s","valorCentavos":0,"categoria":"restaurantes","data":"2026-08-27T12:00:00Z"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}
