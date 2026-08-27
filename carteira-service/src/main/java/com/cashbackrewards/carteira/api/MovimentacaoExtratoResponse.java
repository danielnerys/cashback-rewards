package com.cashbackrewards.carteira.api;

import com.cashbackrewards.carteira.domain.MovimentacaoDeExtrato;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimentacaoExtratoResponse(
        UUID id,
        UUID usuarioId,
        MovimentacaoDeExtrato.Tipo tipo,
        long valorCentavos,
        UUID referenciaId,
        long saldoApos,
        OffsetDateTime criadoEm) {

    public static MovimentacaoExtratoResponse from(MovimentacaoDeExtrato movimentacao, long saldoApos) {
        return new MovimentacaoExtratoResponse(
                movimentacao.getId(),
                movimentacao.getUsuarioId(),
                movimentacao.getTipo(),
                movimentacao.getValorCentavos(),
                movimentacao.getReferenciaId(),
                saldoApos,
                movimentacao.getCriadoEm());
    }
}
