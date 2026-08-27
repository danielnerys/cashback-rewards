package com.cashbackrewards.compras.api;

import com.cashbackrewards.compras.domain.Compra;
import com.cashbackrewards.compras.domain.ComprasService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/compras")
public class ComprasController {

    private final ComprasService comprasService;

    public ComprasController(ComprasService comprasService) {
        this.comprasService = comprasService;
    }

    @PostMapping
    public ResponseEntity<CompraResponse> registrarCompra(@Valid @RequestBody NovaCompraRequest request) {
        Compra compra = comprasService.registrarCompra(
                request.usuarioId(), request.valorCentavos(), request.categoria(), request.data());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompraResponse.from(compra));
    }

    @GetMapping("/{compraId}")
    public CompraResponse consultarCompra(@PathVariable UUID compraId) {
        return CompraResponse.from(comprasService.buscar(compraId));
    }
}
