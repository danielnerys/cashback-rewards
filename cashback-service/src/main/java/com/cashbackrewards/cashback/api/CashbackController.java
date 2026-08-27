package com.cashbackrewards.cashback.api;

import com.cashbackrewards.cashback.domain.CashbackCalculoService;
import com.cashbackrewards.cashback.domain.CreditoDeCashback;
import java.util.Optional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class CashbackController {

    private final CashbackCalculoService cashbackCalculoService;

    public CashbackController(CashbackCalculoService cashbackCalculoService) {
        this.cashbackCalculoService = cashbackCalculoService;
    }

    @PostMapping("/creditos")
    public ResultadoCreditoResponse calcularCredito(
            @jakarta.validation.Valid @RequestBody SolicitacaoCreditoRequest request) {
        Optional<CreditoDeCashback> credito = cashbackCalculoService.calcular(
                request.compraId(), request.usuarioId(), request.valorCentavos(), request.categoria());
        return new ResultadoCreditoResponse(
                credito.isPresent(),
                credito.map(CreditoDeCashbackResponse::from).orElse(null));
    }
}
