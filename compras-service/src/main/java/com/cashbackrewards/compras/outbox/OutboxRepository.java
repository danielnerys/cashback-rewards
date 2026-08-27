package com.cashbackrewards.compras.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {

    List<OutboxEntry> findTop50ByEstadoOrderByCriadoEmAsc(OutboxEntry.Estado estado);
}
