package com.cashbackrewards.cashback.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "regra_cashback")
public class RegraDeCashback {

    @Id
    private UUID id;

    /** {@code null} representa a regra geral (FR-003). */
    @Column(name = "categoria")
    private String categoria;

    @Column(name = "percentual", nullable = false)
    private BigDecimal percentual;

    @Column(name = "valor_minimo_centavos", nullable = false)
    private long valorMinimoCentavos;

    @Column(name = "teto_centavos", nullable = false)
    private long tetoCentavos;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    protected RegraDeCashback() {
    }

    public RegraDeCashback(UUID id, String categoria, BigDecimal percentual, long valorMinimoCentavos,
            long tetoCentavos, boolean ativa) {
        this.id = id;
        this.categoria = categoria;
        this.percentual = percentual;
        this.valorMinimoCentavos = valorMinimoCentavos;
        this.tetoCentavos = tetoCentavos;
        this.ativa = ativa;
    }

    public UUID getId() {
        return id;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public long getValorMinimoCentavos() {
        return valorMinimoCentavos;
    }

    public long getTetoCentavos() {
        return tetoCentavos;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
