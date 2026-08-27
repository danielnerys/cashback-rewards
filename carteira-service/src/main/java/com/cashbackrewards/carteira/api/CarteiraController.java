package com.cashbackrewards.carteira.api;

import com.cashbackrewards.carteira.domain.SaldoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/usuarios/{usuarioId}")
public class CarteiraController {

    private final SaldoService saldoService;

    public CarteiraController(SaldoService saldoService) {
        this.saldoService = saldoService;
    }

    @PostMapping("/creditos")
    public ResponseEntity<MovimentacaoExtratoResponse> aplicarCredito(
            @PathVariable UUID usuarioId, @Valid @RequestBody NovoCreditoRequest request) {
        SaldoService.Resultado resultado = saldoService.aplicarCredito(
                usuarioId, request.creditoId(), request.valorCentavos());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MovimentacaoExtratoResponse.from(resultado.movimentacao(), resultado.saldoApos()));
    }
}
