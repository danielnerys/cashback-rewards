package com.cashbackrewards.carteira.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoDeExtratoRepository extends JpaRepository<MovimentacaoDeExtrato, UUID> {

    List<MovimentacaoDeExtrato> findByUsuarioIdOrderByCriadoEmAsc(UUID usuarioId);
}
