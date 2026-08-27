package com.cashbackrewards.carteira.domain;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O saldo em {@link SaldoDoUsuario} é uma projeção; a fonte de verdade é a
 * sequência de {@link MovimentacaoDeExtrato} (Principle II).
 */
@Service
public class SaldoService {

    private final SaldoDoUsuarioRepository saldoRepository;
    private final MovimentacaoDeExtratoRepository movimentacaoRepository;

    public SaldoService(SaldoDoUsuarioRepository saldoRepository,
            MovimentacaoDeExtratoRepository movimentacaoRepository) {
        this.saldoRepository = saldoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public record Resultado(MovimentacaoDeExtrato movimentacao, long saldoApos) {
    }

    @Transactional
    public Resultado aplicarCredito(UUID usuarioId, UUID creditoId, long valorCentavos) {
        SaldoDoUsuario saldo = buscarOuCriar(usuarioId);
        saldo.aplicar(valorCentavos);
        saldoRepository.save(saldo);

        MovimentacaoDeExtrato movimentacao = new MovimentacaoDeExtrato(
                UUID.randomUUID(), usuarioId, MovimentacaoDeExtrato.Tipo.CREDITO, valorCentavos, creditoId);
        movimentacaoRepository.save(movimentacao);

        return new Resultado(movimentacao, saldo.getSaldoCentavos());
    }

    private SaldoDoUsuario buscarOuCriar(UUID usuarioId) {
        return saldoRepository.findById(usuarioId).orElseGet(() -> new SaldoDoUsuario(usuarioId));
    }
}
