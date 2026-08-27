package com.cashbackrewards.compras.api;

import com.cashbackrewards.compras.domain.Compra;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        UUID usuarioId,
        long valorCentavos,
        String categoria,
        OffsetDateTime data,
        Compra.Estado estado,
        BigDecimal percentualDevolvidoAcumulado) {

    public static CompraResponse from(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getValorCentavos(),
                compra.getCategoria(),
                compra.getData(),
                compra.getEstado(),
                compra.getPercentualDevolvidoAcumulado());
    }
}
