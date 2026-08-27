package com.cashbackrewards.carteira.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record NovoCreditoRequest(
        @NotNull UUID creditoId,
        @NotNull @Positive Long valorCentavos) {
}
