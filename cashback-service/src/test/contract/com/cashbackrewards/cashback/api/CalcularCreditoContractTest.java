package com.cashbackrewards.cashback.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cashbackrewards.cashback.AbstractPostgresIntegrationTest;
import com.cashbackrewards.cashback.domain.RegraDeCashback;
import com.cashbackrewards.cashback.domain.RegraDeCashbackRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato de POST /v1/creditos (contracts/cashback-service.yaml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CalcularCreditoContractTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegraDeCashbackRepository regraRepository;

    @BeforeEach
    void garantirRegraGeral() {
        if (regraRepository.findByCategoriaIsNullAndAtivaTrue().isEmpty()) {
            regraRepository.save(new RegraDeCashback(UUID.randomUUID(), null,
                    new BigDecimal("2.00"), 2000L, 5000L, true));
        }
    }

    @Test
    void calcularCredito_compraElegivel_retorna200ComCreditoPendente() throws Exception {
        mockMvc.perform(post("/v1/creditos")
                        .header("Idempotency-Key", "contract-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compraId":"%s","usuarioId":"%s","valorCentavos":10000,"categoria":"outros"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(true))
                .andExpect(jsonPath("$.credito.valorCalculadoCentavos").value(200))
                .andExpect(jsonPath("$.credito.estado").value("PENDENTE"));
    }

    @Test
    void calcularCredito_abaixoDoPiso_retornaNaoElegivel() throws Exception {
        mockMvc.perform(post("/v1/creditos")
                        .header("Idempotency-Key", "contract-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compraId":"%s","usuarioId":"%s","valorCentavos":500,"categoria":"outros"}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegivel").value(false));
    }
}
