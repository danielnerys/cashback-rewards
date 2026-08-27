package com.cashbackrewards.carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Append-only (Principle II): sem UPDATE/DELETE em nível de aplicação; a
 * migração revoga esses privilégios em nível de banco.
 */
@Entity
@Table(name = "movimentacao_extrato")
public class MovimentacaoDeExtrato {

    public enum Tipo {
        CREDITO,
        ESTORNO,
        RESGATE
    }

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private Tipo tipo;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Column(name = "referencia_id", nullable = false)
    private UUID referenciaId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected MovimentacaoDeExtrato() {
    }

    public MovimentacaoDeExtrato(UUID id, UUID usuarioId, Tipo tipo, long valorCentavos, UUID referenciaId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.valorCentavos = valorCentavos;
        this.referenciaId = referenciaId;
        this.criadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
