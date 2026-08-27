package com.cashbackrewards.cashback.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegraDeCashbackRepository extends JpaRepository<RegraDeCashback, UUID> {

    Optional<RegraDeCashback> findByCategoriaAndAtivaTrue(String categoria);

    Optional<RegraDeCashback> findByCategoriaIsNullAndAtivaTrue();
}
