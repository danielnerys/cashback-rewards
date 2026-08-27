package com.cashbackrewards.carteira.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaldoDoUsuarioRepository extends JpaRepository<SaldoDoUsuario, UUID> {
}
