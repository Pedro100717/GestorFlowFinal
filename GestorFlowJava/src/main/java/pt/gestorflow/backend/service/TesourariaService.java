package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TesourariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;

    // 🚀 Injeções de Segurança
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    // =========================================================================
    // --- 1. GESTÃO DE PENDENTES E LIQUIDAÇÕES (O NOVO FLUXO) ---
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DocumentoPendenteDTO> listarPendentes() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        List<DocumentoPendenteDTO> pendentes = new ArrayList<>();

        List<Venda> vendas = vendaRepository.findAllByUtilizadorIdAndEstadoPagamento(utilizadorId, EstadoPagamento.PENDENTE);
        for (Venda v : vendas) {
            pendentes.add(new DocumentoPendenteDTO(v.getId(), "VENDA", v.getDataVenda(), v.getCliente().getNome(), v.getTotalComIva()));
        }

        List<Compra> compras = compraRepository.findAllByUtilizadorIdAndEstadoPagamento(utilizadorId, EstadoPagamento.PENDENTE);
        for (Compra c : compras) {
            pendentes.add(new DocumentoPendenteDTO(c.getId(), "COMPRA", c.getDataCompra(), c.getFornecedor().getNome(), c.getTotal()));
        }

        // ⚠️ Ponto crítico de performance em escala (ordenar em memória). Ponderar refatorar para Query paginada no futuro.
        pendentes.sort((a, b) -> a.getData().compareTo(b.getData()));
        return pendentes;
    }

    @Transactional
    public void confirmarTransacao(ConfirmarPagamentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(dto.getContaBancariaId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada ou acesso negado."));

        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setDataMovimento(dto.getDataPagamento() != null ? dto.getDataPagamento() : LocalDateTime.now());

        if ("VENDA".equalsIgnoreCase(dto.getTipoDocumento())) {
            Venda venda = vendaRepository.findByIdAndUtilizadorId(dto.getDocumentoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada ou acesso negado."));

            venda.setEstadoPagamento(EstadoPagamento.PAGO);
            venda.setContaBancaria(conta);
            vendaRepository.save(venda);

            mov.setTipo(Movimento.TipoMovimento.CREDITO);
            mov.setValor(venda.getTotalComIva());
            mov.setDescricao("Recebimento Venda #" + venda.getId() + " - " + venda.getCliente().getNome());
            mov.setVenda(venda);
            mov.setCliente(venda.getCliente());

            // O @Version na entidade irá proteger contra Lost Updates aqui
            conta.setSaldo(conta.getSaldo().add(venda.getTotalComIva()));

        } else if ("COMPRA".equalsIgnoreCase(dto.getTipoDocumento())) {
            Compra compra = compraRepository.findByIdAndUtilizadorId(dto.getDocumentoId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Compra não encontrada ou acesso negado."));

            compra.setEstadoPagamento(EstadoPagamento.PAGO);
            compra.setContaBancaria(conta);
            compraRepository.save(compra);

            mov.setTipo(Movimento.TipoMovimento.DEBITO);
            mov.setValor(compra.getTotal());
            mov.setDescricao("Pagamento Compra #" + compra.getId() + " - " + compra.getFornecedor().getNome());
            mov.setCompra(compra);
            mov.setFornecedor(compra.getFornecedor());

            conta.setSaldo(conta.getSaldo().subtract(compra.getTotal()));
        } else {
            throw new IllegalArgumentException("Tipo de documento inválido. Use VENDA ou COMPRA.");
        }

        mov.setSaldoApos(conta.getSaldo());
        contaRepository.save(conta);
        movimentoRepository.save(mov);
    }

    // =========================================================================
    // --- 2. CONTAS BANCÁRIAS (ANTIGO E ESSENCIAL) ---
    // =========================================================================

    @Transactional
    public ContaBancariaResponseDTO criarConta(ContaBancariaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        ContaBancaria c = new ContaBancaria();
        c.setNome(dto.getNome());
        c.setIban(dto.getIban());
        c.setSaldo(dto.getSaldoInicial() != null ? dto.getSaldoInicial() : BigDecimal.ZERO);
        c.setUtilizador(user);

        return converterContaParaDTO(contaRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ContaBancariaResponseDTO> listarContas() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        return contaRepository.findAllByUtilizadorId(utilizadorId)
                .stream().map(this::converterContaParaDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContaBancariaResponseDTO buscarContaPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada ou acesso negado."));
        return converterContaParaDTO(conta);
    }

    // =========================================================================
    // --- 3. MOVIMENTOS E TRANSFERÊNCIAS (ANTIGO E ESSENCIAL) ---
    // =========================================================================

    @Transactional
    public MovimentoResponseDTO registarMovimento(MovimentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(dto.getContaId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta bancária não encontrada ou acesso negado."));

        if (dto.getTipo() == Movimento.TipoMovimento.CREDITO) {
            conta.setSaldo(conta.getSaldo().add(dto.getValor()));
        } else {
            conta.setSaldo(conta.getSaldo().subtract(dto.getValor()));
        }
        contaRepository.save(conta);

        Movimento mov = new Movimento();
        mov.setConta(conta);
        mov.setUtilizador(user);
        mov.setDescricao(dto.getDescricao());
        mov.setTipo(dto.getTipo());
        mov.setValor(dto.getValor());
        mov.setSaldoApos(conta.getSaldo());
        mov.setDataMovimento(LocalDateTime.now());

        return converterParaDTO(movimentoRepository.save(mov));
    }

    @Transactional(readOnly = true)
    public List<MovimentoResponseDTO> obterExtrato(Long contaId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR antes de ir buscar o extrato
        contaRepository.findByIdAndUtilizadorId(contaId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada ou acesso negado."));

        return movimentoRepository.findAllByContaIdOrderByDataMovimentoDesc(contaId)
                .stream().map(this::converterParaDTO).collect(Collectors.toList());
    }

    @Transactional
    public void transferirEntreContas(TransferenciaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        if (dto.getContaOrigemId().equals(dto.getContaDestinoId())) {
            throw new IllegalArgumentException("A conta de origem e destino não podem ser a mesma.");
        }

        ContaBancaria origem = contaRepository.findByIdAndUtilizadorId(dto.getContaOrigemId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de origem não encontrada ou acesso negado."));

        ContaBancaria destino = contaRepository.findByIdAndUtilizadorId(dto.getContaDestinoId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta de destino não encontrada ou acesso negado."));

        LocalDateTime agora = LocalDateTime.now();

        // Débito (Saída)
        origem.setSaldo(origem.getSaldo().subtract(dto.getValor()));
        contaRepository.save(origem);

        Movimento movSaida = new Movimento();
        movSaida.setConta(origem);
        movSaida.setUtilizador(user);
        movSaida.setDataMovimento(agora);
        String descSaida = (dto.getDescricao() != null && !dto.getDescricao().isBlank())
                ? dto.getDescricao() : "Transferência para " + destino.getNome();
        movSaida.setDescricao(descSaida);
        movSaida.setTipo(Movimento.TipoMovimento.DEBITO);
        movSaida.setValor(dto.getValor());
        movSaida.setSaldoApos(origem.getSaldo());
        movimentoRepository.save(movSaida);

        // Crédito (Entrada)
        destino.setSaldo(destino.getSaldo().add(dto.getValor()));
        contaRepository.save(destino);

        Movimento movEntrada = new Movimento();
        movEntrada.setConta(destino);
        movEntrada.setUtilizador(user);
        movEntrada.setDataMovimento(agora);
        String descEntrada = (dto.getDescricao() != null && !dto.getDescricao().isBlank())
                ? dto.getDescricao() : "Transferência recebida de " + origem.getNome();
        movEntrada.setDescricao(descEntrada);
        movEntrada.setTipo(Movimento.TipoMovimento.CREDITO);
        movEntrada.setValor(dto.getValor());
        movEntrada.setSaldoApos(destino.getSaldo());
        movimentoRepository.save(movEntrada);
    }

    // =========================================================================
    // --- CONVERSORES ---
    // =========================================================================

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

        if (mov.getCompra() != null) dto.setCompraId(mov.getCompra().getId());
        if (mov.getVenda() != null) dto.setVendaId(mov.getVenda().getId());
        if (mov.getFornecedor() != null) dto.setFornecedorNome(mov.getFornecedor().getNome());
        if (mov.getCliente() != null) dto.setClienteNome(mov.getCliente().getNome());

        return dto;
    }
}