package com.cashbackrewards.cashback.api;

import com.cashbackrewards.cashback.domain.CreditoDeCashback;
import java.util.UUID;

public record CreditoDeCashbackResponse(
        UUID id,
        UUID compraId,
        UUID usuarioId,
        long valorCalculadoCentavos,
        CreditoDeCashback.Estado estado) {

    public static CreditoDeCashbackResponse from(CreditoDeCashback credito) {
        return new CreditoDeCashbackResponse(
                credito.getId(),
                credito.getCompraId(),
                credito.getUsuarioId(),
                credito.getValorCalculadoCentavos(),
                credito.getEstado());
    }
}
