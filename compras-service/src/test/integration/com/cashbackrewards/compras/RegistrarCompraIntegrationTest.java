package com.cashbackrewards.compras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cashbackrewards.compras.domain.Compra;
import com.cashbackrewards.compras.domain.CompraRepository;
import com.cashbackrewards.compras.outbox.OutboxEntry;
import com.cashbackrewards.compras.outbox.OutboxRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RegistrarCompraIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    void registrarCompra_persisteCompraAtivaEEnfileiraOutbox() throws Exception {
        String usuarioId = UUID.randomUUID().toString();
        String idempotencyKey = "int-" + UUID.randomUUID();

        mockMvc.perform(post("/v1/compras")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":"%s","valorCentavos":15000,"categoria":"geral","data":"2026-08-27T12:00:00Z"}
                                """.formatted(usuarioId)))
                .andExpect(status().isCreated());

        List<Compra> comprasDoUsuario = compraRepository.findAll().stream()
                .filter(c -> c.getUsuarioId().equals(UUID.fromString(usuarioId)))
                .toList();
        assertThat(comprasDoUsuario).hasSize(1);
        Compra compra = comprasDoUsuario.get(0);
        assertThat(compra.getEstado()).isEqualTo(Compra.Estado.ATIVA);

        List<OutboxEntry> outboxDaCompra = outboxRepository.findAll().stream()
                .filter(o -> o.getIdempotencyKey().equals(compra.getId().toString()))
                .toList();
        assertThat(outboxDaCompra).hasSize(1);
        assertThat(outboxDaCompra.get(0).getTipoEvento()).isEqualTo(OutboxEntry.TipoEvento.COMPRA_REGISTRADA);
        assertThat(outboxDaCompra.get(0).getPayload()).contains(usuarioId);
    }
}
