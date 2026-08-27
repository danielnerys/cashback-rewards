package com.cashbackrewards.cashback.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record SolicitacaoCreditoRequest(
        @NotNull UUID compraId,
        @NotNull UUID usuarioId,
        @NotNull @Positive Long valorCentavos,
        @NotBlank String categoria) {
}
