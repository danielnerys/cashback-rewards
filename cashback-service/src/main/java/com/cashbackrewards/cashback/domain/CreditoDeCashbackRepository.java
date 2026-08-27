package com.cashbackrewards.cashback.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditoDeCashbackRepository extends JpaRepository<CreditoDeCashback, UUID> {

    Optional<CreditoDeCashback> findByCompraId(UUID compraId);
}
