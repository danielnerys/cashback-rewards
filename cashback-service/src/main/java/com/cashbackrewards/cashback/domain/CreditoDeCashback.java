package com.cashbackrewards.cashback.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "credito_cashback")
public class CreditoDeCashback {

    public enum Estado {
        PENDENTE,
        CONCLUIDO
    }

    @Id
    private UUID id;

    @Column(name = "compra_id", nullable = false)
    private UUID compraId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "valor_calculado_centavos", nullable = false)
    private long valorCalculadoCentavos;

    @Column(name = "regra_id", nullable = false)
    private UUID regraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected CreditoDeCashback() {
    }

    public CreditoDeCashback(UUID id, UUID compraId, UUID usuarioId, long valorCalculadoCentavos, UUID regraId) {
        this.id = id;
        this.compraId = compraId;
        this.usuarioId = usuarioId;
        this.valorCalculadoCentavos = valorCalculadoCentavos;
        this.regraId = regraId;
        this.estado = Estado.PENDENTE;
        this.criadoEm = OffsetDateTime.now();
    }

    public void marcarConcluido() {
        this.estado = Estado.CONCLUIDO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompraId() {
        return compraId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public long getValorCalculadoCentavos() {
        return valorCalculadoCentavos;
    }

    public UUID getRegraId() {
        return regraId;
    }

    public Estado getEstado() {
        return estado;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
