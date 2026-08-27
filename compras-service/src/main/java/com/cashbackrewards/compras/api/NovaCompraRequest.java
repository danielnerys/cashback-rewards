package com.cashbackrewards.compras.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NovaCompraRequest(
        @NotNull UUID usuarioId,
        @NotNull @Positive Long valorCentavos,
        @NotBlank String categoria,
        @NotNull OffsetDateTime data) {
}
