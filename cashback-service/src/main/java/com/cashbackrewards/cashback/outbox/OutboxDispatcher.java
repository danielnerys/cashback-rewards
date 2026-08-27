package com.cashbackrewards.cashback.outbox;

import com.cashbackrewards.cashback.outbox.OutboxEntry.Estado;
import com.cashbackrewards.cashback.outbox.OutboxEntry.TipoEvento;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Lê entradas pendentes do outbox e chama o carteira-service com uma
 * requisição/resposta síncrona comum (research.md #1). Satisfaz FR-018/FR-019:
 * o crédito/estorno calculado nunca esperou por esta chamada para responder
 * ao compras-service.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final RestClient carteiraServiceRestClient;
    private final RetryTemplate carteiraServiceRetryTemplate;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(OutboxRepository outboxRepository, RestClient carteiraServiceRestClient,
            RetryTemplate carteiraServiceRetryTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.carteiraServiceRestClient = carteiraServiceRestClient;
        this.carteiraServiceRetryTemplate = carteiraServiceRetryTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.dispatch-fixed-delay-ms}")
    public void despachar() {
        List<OutboxEntry> pendentes = outboxRepository.findTop50ByEstadoOrderByCriadoEmAsc(Estado.PENDENTE);
        for (OutboxEntry entrada : pendentes) {
            despacharUm(entrada);
        }
    }

    @Transactional
    void despacharUm(OutboxEntry entrada) {
        try {
            String caminho = caminhoPara(entrada);
            carteiraServiceRetryTemplate.execute(contexto -> carteiraServiceRestClient.post()
                    .uri(caminho)
                    .header("Idempotency-Key", entrada.getIdempotencyKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(entrada.getPayload())
                    .retrieve()
                    .toBodilessEntity());
            entrada.marcarEnviado();
            outboxRepository.save(entrada);
        } catch (Exception e) {
            entrada.registrarTentativaFalha();
            outboxRepository.save(entrada);
            log.warn("Falha ao despachar outbox {} para carteira-service (tentativa {}): {}",
                    entrada.getId(), entrada.getTentativas(), e.getMessage());
        }
    }

    /**
     * O usuarioId é parte do caminho no carteira-service (path param), não
     * do corpo isolado — por isso é extraído do payload já persistido no
     * outbox em vez de estar fixo por tipo de evento.
     */
    private String caminhoPara(OutboxEntry entrada) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(entrada.getPayload());
        } catch (Exception e) {
            throw new IllegalStateException("Payload inválido no outbox " + entrada.getId(), e);
        }
        String usuarioId = payload.path("usuarioId").asText();
        return switch (entrada.getTipoEvento()) {
            case CREDITO_CALCULADO -> "/v1/usuarios/%s/creditos".formatted(usuarioId);
            case ESTORNO_CALCULADO -> "/v1/usuarios/%s/estornos".formatted(usuarioId);
        };
    }
}
