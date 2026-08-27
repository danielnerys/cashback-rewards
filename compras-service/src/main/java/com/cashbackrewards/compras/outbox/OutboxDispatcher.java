package com.cashbackrewards.compras.outbox;

import com.cashbackrewards.compras.outbox.OutboxEntry.Estado;
import com.cashbackrewards.compras.outbox.OutboxEntry.TipoEvento;
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
 * Lê entradas pendentes do outbox e chama o cashback-service com uma
 * requisição/resposta síncrona comum (sem accept-and-acknowledge), decidindo
 * apenas o MOMENTO da chamada — nunca o formato da resposta (research.md #1).
 * Satisfaz FR-018/FR-019: o registro original nunca esperou por esta chamada.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final RestClient cashbackServiceRestClient;
    private final RetryTemplate cashbackServiceRetryTemplate;

    public OutboxDispatcher(OutboxRepository outboxRepository, RestClient cashbackServiceRestClient,
            RetryTemplate cashbackServiceRetryTemplate) {
        this.outboxRepository = outboxRepository;
        this.cashbackServiceRestClient = cashbackServiceRestClient;
        this.cashbackServiceRetryTemplate = cashbackServiceRetryTemplate;
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
        String caminho = caminhoPara(entrada.getTipoEvento());
        try {
            cashbackServiceRetryTemplate.execute(contexto -> cashbackServiceRestClient.post()
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
            log.warn("Falha ao despachar outbox {} para cashback-service (tentativa {}): {}",
                    entrada.getId(), entrada.getTentativas(), e.getMessage());
        }
    }

    private static String caminhoPara(TipoEvento tipoEvento) {
        return switch (tipoEvento) {
            case COMPRA_REGISTRADA -> "/v1/creditos";
            case CANCELAMENTO_REGISTRADO, DEVOLUCAO_REGISTRADA -> "/v1/estornos";
        };
    }
}
