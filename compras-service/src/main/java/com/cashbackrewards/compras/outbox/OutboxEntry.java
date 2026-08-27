package com.cashbackrewards.compras.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox")
public class OutboxEntry {

    public enum TipoEvento {
        COMPRA_REGISTRADA,
        CANCELAMENTO_REGISTRADO,
        DEVOLUCAO_REGISTRADA
    }

    public enum Estado {
        PENDENTE,
        ENVIADO
    }

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEvento tipoEvento;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private Estado estado;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    protected OutboxEntry() {
    }

    public OutboxEntry(UUID id, TipoEvento tipoEvento, String payload, String idempotencyKey) {
        this.id = id;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.idempotencyKey = idempotencyKey;
        this.estado = Estado.PENDENTE;
        this.tentativas = 0;
        OffsetDateTime agora = OffsetDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public void marcarEnviado() {
        this.estado = Estado.ENVIADO;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void registrarTentativaFalha() {
        this.tentativas++;
        this.atualizadoEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Estado getEstado() {
        return estado;
    }

    public int getTentativas() {
        return tentativas;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
