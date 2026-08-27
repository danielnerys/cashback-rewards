package com.cashbackrewards.compras.domain;

import com.cashbackrewards.compras.outbox.OutboxEntry;
import com.cashbackrewards.compras.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra compras, cancelamentos e devoluções, e enfileira o evento
 * correspondente no outbox na mesma transação (FR-018: o registro nunca
 * espera pela disponibilidade do cashback-service).
 */
@Service
public class ComprasService {

    private final CompraRepository compraRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public ComprasService(CompraRepository compraRepository, OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.compraRepository = compraRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Compra registrarCompra(UUID usuarioId, long valorCentavos, String categoria, OffsetDateTime data) {
        Compra compra = new Compra(UUID.randomUUID(), usuarioId, valorCentavos, categoria, data);
        compraRepository.save(compra);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("compraId", compra.getId());
        payload.put("usuarioId", compra.getUsuarioId());
        payload.put("valorCentavos", compra.getValorCentavos());
        payload.put("categoria", compra.getCategoria());
        enfileirarOutbox(OutboxEntry.TipoEvento.COMPRA_REGISTRADA, payload, compra.getId().toString());

        return compra;
    }

    public Compra buscar(UUID compraId) {
        return compraRepository.findById(compraId)
                .orElseThrow(() -> new NoSuchElementException("Compra " + compraId + " não encontrada"));
    }

    private void enfileirarOutbox(OutboxEntry.TipoEvento tipoEvento, Map<String, Object> payload,
            String idempotencyKey) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            outboxRepository.save(new OutboxEntry(UUID.randomUUID(), tipoEvento, payloadJson, idempotencyKey));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do outbox", e);
        }
    }
}
