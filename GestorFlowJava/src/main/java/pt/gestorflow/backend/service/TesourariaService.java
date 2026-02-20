package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.ContaBancariaDTO;
import pt.gestorflow.backend.dto.MovimentoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TesourariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // --- Contas Bancárias ---

    public ContaBancaria criarConta(ContaBancariaDTO dto) {
        ContaBancaria c = new ContaBancaria();
        c.setNome(dto.getNome());
        c.setIban(dto.getIban());

        // Se vier null, mete zero. Se vier valor, usa-o.
        c.setSaldo(dto.getSaldoInicial() != null ? dto.getSaldoInicial() : BigDecimal.ZERO);

        c.setUtilizador(getUtilizadorLogado());

        return contaRepository.save(c);
    }

    public List<ContaBancaria> listarContas() {
        return contaRepository.findAllByUtilizadorId(getUtilizadorLogado().getId());
    }

    // --- Movimentos ---

    @Transactional
    public Movimento registarMovimento(MovimentoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        ContaBancaria conta = contaRepository.findById(dto.getContaId())
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada"));

        // Validar se a conta pertence ao user (Segurança extra)
        if (!conta.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Não tem permissão para movimentar esta conta.");
        }

        // 1. Atualizar Saldo
        if (dto.getTipo() == Movimento.TipoMovimento.CREDITO) {
            conta.setSaldo(conta.getSaldo().add(dto.getValor()));
        } else {
            conta.setSaldo(conta.getSaldo().subtract(dto.getValor()));
        }
        contaRepository.save(conta); // Guarda o novo saldo

        // 2. Criar Movimento
        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setDescricao(dto.getDescricao());
        mov.setTipo(dto.getTipo());
        mov.setValor(dto.getValor());
        mov.setSaldoApos(conta.getSaldo()); // Fica registado quanto ficou na conta

        return movimentoRepository.save(mov);
    }

    public List<Movimento> obterExtrato(Long contaId) {
        // Validação de segurança simples
        ContaBancaria conta = contaRepository.findById(contaId).orElseThrow();
        if (!conta.getUtilizador().getId().equals(getUtilizadorLogado().getId())) {
            throw new RuntimeException("Sem permissão.");
        }
        return movimentoRepository.findAllByContaIdOrderByDataMovimentoDesc(contaId);
    }
}