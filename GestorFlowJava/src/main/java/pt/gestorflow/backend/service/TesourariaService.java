package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.ContaBancariaDTO;
import pt.gestorflow.backend.dto.ContaBancariaResponseDTO; // NOVO IMPORT
import pt.gestorflow.backend.dto.MovimentoDTO;
import pt.gestorflow.backend.dto.MovimentoResponseDTO;
import pt.gestorflow.backend.dto.TransferenciaDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TesourariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // --- Contas Bancárias ---

    public ContaBancariaResponseDTO criarConta(ContaBancariaDTO dto) {
        ContaBancaria c = new ContaBancaria();
        c.setNome(dto.getNome());
        c.setIban(dto.getIban());

        c.setSaldo(dto.getSaldoInicial() != null ? dto.getSaldoInicial() : BigDecimal.ZERO);
        c.setUtilizador(getUtilizadorLogado());

        ContaBancaria contaGuardada = contaRepository.save(c);

        // Retorna o DTO em vez da Entidade!
        return converterContaParaDTO(contaGuardada);
    }

    @Transactional(readOnly = true)
    public List<ContaBancariaResponseDTO> listarContas() {
        List<ContaBancaria> contas = contaRepository.findAllByUtilizadorId(getUtilizadorLogado().getId());

        // Converte a lista da Base de Dados para uma lista de DTOs limpos
        return contas.stream()
                .map(this::converterContaParaDTO)
                .collect(Collectors.toList());
    }

    // --- Movimentos ---

    @Transactional
    public MovimentoResponseDTO registarMovimento(MovimentoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        ContaBancaria conta = contaRepository.findById(dto.getContaId())
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada"));

        if (!conta.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Não tem permissão para movimentar esta conta.");
        }

        // 1. Atualizar Saldo
        if (dto.getTipo() == Movimento.TipoMovimento.CREDITO) {
            conta.setSaldo(conta.getSaldo().add(dto.getValor()));
        } else {
            conta.setSaldo(conta.getSaldo().subtract(dto.getValor()));
        }
        contaRepository.save(conta);

        // 2. Criar Movimento
        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setDescricao(dto.getDescricao());
        mov.setTipo(dto.getTipo());
        mov.setValor(dto.getValor());
        mov.setSaldoApos(conta.getSaldo());

        Movimento movGuardado = movimentoRepository.save(mov);

        // Retorna o envelope limpo!
        return converterParaDTO(movGuardado);
    }

    @Transactional(readOnly = true)
    public List<MovimentoResponseDTO> obterExtrato(Long contaId) {
        ContaBancaria conta = contaRepository.findById(contaId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada"));

        if (!conta.getUtilizador().getId().equals(getUtilizadorLogado().getId())) {
            throw new RuntimeException("Sem permissão.");
        }

        List<Movimento> movimentosDaBaseDeDados = movimentoRepository.findAllByContaIdOrderByDataMovimentoDesc(contaId);

        // Transforma a lista de Entidades numa lista de DTOs limpos
        return movimentosDaBaseDeDados.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    // --- Transferências ---

    @Transactional
    public void transferirEntreContas(TransferenciaDTO dto) {
        Utilizador user = getUtilizadorLogado();

        if (dto.getContaOrigemId().equals(dto.getContaDestinoId())) {
            throw new IllegalArgumentException("A conta de origem e destino não podem ser a mesma.");
        }

        ContaBancaria origem = contaRepository.findById(dto.getContaOrigemId())
                .orElseThrow(() -> new EntityNotFoundException("Conta de origem não encontrada"));
        ContaBancaria destino = contaRepository.findById(dto.getContaDestinoId())
                .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada"));

        if (!origem.getUtilizador().getId().equals(user.getId()) || !destino.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Sem permissão para movimentar estas contas.");
        }

        // 1. Tira da Origem
        origem.setSaldo(origem.getSaldo().subtract(dto.getValor()));
        contaRepository.save(origem);

        Movimento movSaida = new Movimento();
        movSaida.setConta(origem);
        movSaida.setUtilizador(user);
        String descSaida = (dto.getDescricao() != null && !dto.getDescricao().isBlank())
                ? dto.getDescricao() : "Transferência para " + destino.getNome();
        movSaida.setDescricao(descSaida);
        movSaida.setTipo(Movimento.TipoMovimento.DEBITO);
        movSaida.setValor(dto.getValor());
        movSaida.setSaldoApos(origem.getSaldo());
        movimentoRepository.save(movSaida);

        // 2. Coloca no Destino
        destino.setSaldo(destino.getSaldo().add(dto.getValor()));
        contaRepository.save(destino);

        Movimento movEntrada = new Movimento();
        movEntrada.setConta(destino);
        movEntrada.setUtilizador(user);
        String descEntrada = (dto.getDescricao() != null && !dto.getDescricao().isBlank())
                ? dto.getDescricao() : "Transferência recebida de " + origem.getNome();
        movEntrada.setDescricao(descEntrada);
        movEntrada.setTipo(Movimento.TipoMovimento.CREDITO);
        movEntrada.setValor(dto.getValor());
        movEntrada.setSaldoApos(destino.getSaldo());
        movimentoRepository.save(movEntrada);
    }

    // ==========================================
    // MAGIA: OS CONVERSORES PARA OS DTOs DE SAÍDA
    // ==========================================

    private ContaBancariaResponseDTO converterContaParaDTO(ContaBancaria conta) {
        ContaBancariaResponseDTO dto = new ContaBancariaResponseDTO();
        dto.setId(conta.getId());
        dto.setNome(conta.getNome());
        dto.setIban(conta.getIban());
        dto.setSaldo(conta.getSaldo());
        return dto;
    }

    private MovimentoResponseDTO converterParaDTO(Movimento mov) {
        MovimentoResponseDTO dto = new MovimentoResponseDTO();
        dto.setId(mov.getId());

        if (mov.getDataMovimento() != null) {
            dto.setDataMovimento(mov.getDataMovimento().toString());
        }

        dto.setDescricao(mov.getDescricao());
        dto.setTipo(mov.getTipo().name());
        dto.setValor(mov.getValor());

        // Verifica as relações de forma segura para não dar NullPointerException
        if (mov.getCompra() != null) {
            dto.setCompraId(mov.getCompra().getId());
        }
        if (mov.getVenda() != null) {
            dto.setVendaId(mov.getVenda().getId());
        }
        if (mov.getFornecedor() != null) {
            dto.setFornecedorNome(mov.getFornecedor().getNome());
        }
        if (mov.getCliente() != null) {
            dto.setClienteNome(mov.getCliente().getNome());
        }

        return dto;
    }
}