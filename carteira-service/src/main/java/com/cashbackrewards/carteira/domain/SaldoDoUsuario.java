package com.cashbackrewards.carteira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projeção materializada para leitura rápida do saldo. A fonte de verdade é
 * a soma das MovimentacaoDeExtrato do usuário (Principle II — Ledger
 * Integrity); este valor deve sempre ser reconciliável por replay.
 */
@Entity
@Table(name = "saldo_usuario")
public class SaldoDoUsuario {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "saldo_centavos", nullable = false)
    private long saldoCentavos;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected SaldoDoUsuario() {
    }

    public SaldoDoUsuario(UUID usuarioId) {
        this.usuarioId = usuarioId;
        this.saldoCentavos = 0L;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void aplicar(long deltaCentavos) {
        this.saldoCentavos += deltaCentavos;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public long getSaldoCentavos() {
        return saldoCentavos;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
