package com.cashbackrewards.cashback.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.cashbackrewards.cashback.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Cobre os 4 Acceptance Scenarios da User Story 1 (spec.md): regra de
 * categoria, teto máximo, piso de elegibilidade e regra geral como fallback.
 */
@SpringBootTest
class CalculoCreditoIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RegraDeCashbackRepository regraRepository;
    @Autowired
    private CreditoDeCashbackRepository creditoRepository;
    @Autowired
    private CashbackCalculoService cashbackCalculoService;

    @BeforeEach
    void configurarRegras() {
        creditoRepository.deleteAll();
        regraRepository.deleteAll();
        regraRepository.save(new RegraDeCashback(UUID.randomUUID(), "restaurantes",
                new BigDecimal("5.00"), 2000L, 5000L, true));
        regraRepository.save(new RegraDeCashback(UUID.randomUUID(), null,
                new BigDecimal("2.00"), 2000L, 5000L, true));
    }

    @Test
    void compraDeCemReaisEmRestaurantesGeraCincoReaisDeCashback() {
        Optional<CreditoDeCashback> credito = cashbackCalculoService.calcular(
                UUID.randomUUID(), UUID.randomUUID(), 10000L, "restaurantes");

        assertThat(credito).isPresent();
        assertThat(credito.get().getValorCalculadoCentavos()).isEqualTo(500L);
    }

    @Test
    void cashbackCalculadoAcimaDoTetoELimitadoAoTeto() {
        Optional<CreditoDeCashback> credito = cashbackCalculoService.calcular(
                UUID.randomUUID(), UUID.randomUUID(), 500000L, "outros");

        assertThat(credito).isPresent();
        assertThat(credito.get().getValorCalculadoCentavos()).isEqualTo(5000L);
    }

    @Test
    void compraAbaixoDoPisoNaoGeraCredito() {
        Optional<CreditoDeCashback> credito = cashbackCalculoService.calcular(
                UUID.randomUUID(), UUID.randomUUID(), 1000L, "restaurantes");

        assertThat(credito).isEmpty();
        assertThat(creditoRepository.count()).isZero();
    }

    @Test
    void categoriaSemRegraEspecificaUsaRegraGeral() {
        Optional<CreditoDeCashback> credito = cashbackCalculoService.calcular(
                UUID.randomUUID(), UUID.randomUUID(), 10000L, "categoria-inexistente");

        assertThat(credito).isPresent();
        assertThat(credito.get().getValorCalculadoCentavos()).isEqualTo(200L);
    }
}
