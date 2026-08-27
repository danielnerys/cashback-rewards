package com.cashbackrewards.carteira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.cashbackrewards.carteira.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AplicarCreditoIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SaldoService saldoService;
    @Autowired
    private SaldoDoUsuarioRepository saldoRepository;
    @Autowired
    private MovimentacaoDeExtratoRepository movimentacaoRepository;

    @Test
    void aplicarCredito_atualizaSaldoECriaMovimentacaoDeCredito() {
        UUID usuarioId = UUID.randomUUID();
        UUID creditoId = UUID.randomUUID();

        saldoService.aplicarCredito(usuarioId, creditoId, 500L);

        SaldoDoUsuario saldo = saldoRepository.findById(usuarioId).orElseThrow();
        assertThat(saldo.getSaldoCentavos()).isEqualTo(500L);

        var movimentacoes = movimentacaoRepository.findByUsuarioIdOrderByCriadoEmAsc(usuarioId);
        assertThat(movimentacoes).hasSize(1);
        assertThat(movimentacoes.get(0).getTipo()).isEqualTo(MovimentacaoDeExtrato.Tipo.CREDITO);
        assertThat(movimentacoes.get(0).getValorCentavos()).isEqualTo(500L);
        assertThat(movimentacoes.get(0).getReferenciaId()).isEqualTo(creditoId);
    }
}
