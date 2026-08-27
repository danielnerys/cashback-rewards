package com.cashbackrewards.compras.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, UUID> {
}
