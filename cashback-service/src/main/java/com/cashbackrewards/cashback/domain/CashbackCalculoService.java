package com.cashbackrewards.cashback.domain;

import com.cashbackrewards.cashback.outbox.OutboxEntry;
import com.cashbackrewards.cashback.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica a regra de categoria (ou geral) com piso e teto (FR-002 a FR-004) e
 * enfileira o crédito calculado no outbox para o carteira-service
 * (research.md #1).
 */
@Service
public class CashbackCalculoService {

    private final RegraDeCashbackRepository regraRepository;
    private final CreditoDeCashbackRepository creditoRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public CashbackCalculoService(RegraDeCashbackRepository regraRepository,
            CreditoDeCashbackRepository creditoRepository, OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.regraRepository = regraRepository;
        this.creditoRepository = creditoRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<CreditoDeCashback> calcular(UUID compraId, UUID usuarioId, long valorCentavos,
            String categoria) {
        RegraDeCashback regra = regraRepository.findByCategoriaAndAtivaTrue(categoria)
                .or(() -> regraRepository.findByCategoriaIsNullAndAtivaTrue())
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma regra de cashback geral configurada; sistema mal configurado"));

        if (valorCentavos < regra.getValorMinimoCentavos()) {
            return Optional.empty();
        }

        long valorCalculado = calcularValor(valorCentavos, regra);

        CreditoDeCashback credito = new CreditoDeCashback(UUID.randomUUID(), compraId, usuarioId,
                valorCalculado, regra.getId());
        creditoRepository.save(credito);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("usuarioId", credito.getUsuarioId());
        payload.put("creditoId", credito.getId());
        payload.put("valorCentavos", credito.getValorCalculadoCentavos());
        enfileirarOutbox(OutboxEntry.TipoEvento.CREDITO_CALCULADO, payload, credito.getId().toString());

        return Optional.of(credito);
    }

    private static long calcularValor(long valorCentavos, RegraDeCashback regra) {
        BigDecimal bruto = BigDecimal.valueOf(valorCentavos)
                .multiply(regra.getPercentual())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        long tetoAplicado = Math.min(bruto.longValueExact(), regra.getTetoCentavos());
        return tetoAplicado;
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
