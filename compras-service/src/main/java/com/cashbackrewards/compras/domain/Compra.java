package com.cashbackrewards.compras.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "compra")
public class Compra {

    public enum Estado {
        ATIVA,
        CANCELADA,
        PARCIALMENTE_DEVOLVIDA,
        TOTALMENTE_DEVOLVIDA
    }

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Column(name = "categoria", nullable = false)
    private String categoria;

    @Column(name = "data", nullable = false)
    private OffsetDateTime data;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado;

    @Column(name = "percentual_devolvido_acumulado", nullable = false)
    private BigDecimal percentualDevolvidoAcumulado;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Compra() {
    }

    public Compra(UUID id, UUID usuarioId, long valorCentavos, String categoria, OffsetDateTime data) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.valorCentavos = valorCentavos;
        this.categoria = categoria;
        this.data = data;
        this.estado = Estado.ATIVA;
        this.percentualDevolvidoAcumulado = BigDecimal.ZERO;
        this.criadoEm = OffsetDateTime.now();
    }

    public void cancelar() {
        if (estado == Estado.CANCELADA || estado == Estado.TOTALMENTE_DEVOLVIDA) {
            throw new EstadoInvalidoException("Compra " + id + " já está " + estado);
        }
        this.estado = Estado.CANCELADA;
    }

    public void registrarDevolucao(BigDecimal percentualDevolvido) {
        if (estado == Estado.CANCELADA || estado == Estado.TOTALMENTE_DEVOLVIDA) {
            throw new EstadoInvalidoException("Compra " + id + " já está " + estado);
        }
        BigDecimal novoAcumulado = this.percentualDevolvidoAcumulado.add(percentualDevolvido);
        if (novoAcumulado.compareTo(new BigDecimal("100")) > 0) {
            throw new EstadoInvalidoException(
                    "Devolução excederia 100%% do valor da compra " + id + " (acumulado seria " + novoAcumulado + "%)");
        }
        this.percentualDevolvidoAcumulado = novoAcumulado;
        this.estado = novoAcumulado.compareTo(new BigDecimal("100")) == 0
                ? Estado.TOTALMENTE_DEVOLVIDA
                : Estado.PARCIALMENTE_DEVOLVIDA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public String getCategoria() {
        return categoria;
    }

    public OffsetDateTime getData() {
        return data;
    }

    public Estado getEstado() {
        return estado;
    }

    public BigDecimal getPercentualDevolvidoAcumulado() {
        return percentualDevolvidoAcumulado;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
