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
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TesourariaService {

    private final ContaBancariaRepository contaRepository;
    private final MovimentoRepository movimentoRepository;
    private final CompraRepository compraRepository;
    private final VendaRepository vendaRepository;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;
    private final UtilizadorRepository utilizadorRepository;

    // 🚀 NOVO: Repositório dos planos para o motor matemático
    private final MovimentoPlaneadoRepository movimentoPlaneadoRepository;
    private final AuthService authService;

    // =========================================================================
    // --- 🚀 1. SIMULADOR DE TESOURARIA (MOTOR DE PROJEÇÃO INDUSTRIAL) ---
    // =========================================================================

    @Transactional(readOnly = true)
    public SimuladorTesourariaDTO obterSimulacao() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        BigDecimal saldoInicial = contaRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(ContaBancaria::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🚀 PASSO 1: Criar o "Horizonte" (Os próximos 12 meses garantidos)
        Map<YearMonth, BigDecimal> fluxoMensal = new TreeMap<>();
        YearMonth mesCorrente = YearMonth.now();
        for (int i = 0; i <= 12; i++) {
            fluxoMensal.put(mesCorrente.plusMonths(i), BigDecimal.ZERO);
        }

        List<EstadoPagamento> estadosPendentes = List.of(EstadoPagamento.PENDENTE, EstadoPagamento.PARCIALMENTE_PAGO);
        List<Venda> vendasPendentes = vendaRepository.findAllByUtilizadorIdAndEstadoPagamentoIn(utilizadorId, estadosPendentes);
        List<Compra> comprasPendentes = compraRepository.findAllByUtilizadorIdAndEstadoPagamentoIn(utilizadorId, estadosPendentes);

        // 🚀 PASSO 2: Injetar a Realidade (Faturas Pendentes Reais)
        for (Venda v : vendasPendentes) {
            YearMonth chave = YearMonth.from(v.getDataVencimento() != null ? v.getDataVencimento() : LocalDateTime.now());
            BigDecimal valorPendente = v.getTotalComIva().subtract(v.getValorPago());
            fluxoMensal.put(chave, fluxoMensal.getOrDefault(chave, BigDecimal.ZERO).add(valorPendente));
        }

        for (Compra c : comprasPendentes) {
            YearMonth chave = YearMonth.from(c.getDataVencimento() != null ? c.getDataVencimento() : LocalDateTime.now());
            BigDecimal valorPendente = c.getTotal().subtract(c.getValorPago());
            fluxoMensal.put(chave, fluxoMensal.getOrDefault(chave, BigDecimal.ZERO).subtract(valorPendente));
        }

        // 🚀 PASSO 3: O MOTOR MATEMÁTICO DE RECORRÊNCIA (Injetar o Futuro Planeado)
        List<MovimentoPlaneado> planosAtivos = movimentoPlaneadoRepository.findAllByUtilizadorIdAndAtivoTrue(utilizadorId);

        for (MovimentoPlaneado plan : planosAtivos) {
            for (Map.Entry<YearMonth, BigDecimal> mesHorizonte : fluxoMensal.entrySet()) {
                if (deveAplicarPlanoNesteMes(plan, mesHorizonte.getKey())) {

                    // Semanal: multiplicamos o valor base por 4 semanas médias num mês
                    BigDecimal valorAInjetar = plan.getFrequencia() == FrequenciaMovimento.SEMANAL
                            ? plan.getValorComIva().multiply(BigDecimal.valueOf(4))
                            : plan.getValorComIva();

                    // TODO: Futura Lógica de Conciliação Automática (O Triângulo Dourado)

                    if (plan.getTipo() == TipoMovimentoPlaneado.ENTRADA) {
                        fluxoMensal.put(mesHorizonte.getKey(), mesHorizonte.getValue().add(valorAInjetar));
                    } else {
                        fluxoMensal.put(mesHorizonte.getKey(), mesHorizonte.getValue().subtract(valorAInjetar));
                    }
                }
            }
        }

        // 🚀 PASSO 4: Gerar os Pontos do Gráfico
        List<SimuladorTesourariaDTO.PontoSimulacao> pontos = new ArrayList<>();
        BigDecimal saldoAcumulado = saldoInicial;
        pontos.add(new SimuladorTesourariaDTO.PontoSimulacao("Atual", saldoInicial));

        for (Map.Entry<YearMonth, BigDecimal> entrada : fluxoMensal.entrySet()) {
            saldoAcumulado = saldoAcumulado.add(entrada.getValue());
            String mesLabel = entrada.getKey().getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "PT")) + "/" + entrada.getKey().getYear();
            pontos.add(new SimuladorTesourariaDTO.PontoSimulacao(mesLabel, saldoAcumulado));
        }

        return new SimuladorTesourariaDTO(saldoInicial, pontos);
    }

    // --- ALGORITMO DE CÁLCULO DE RECORRÊNCIA ---
    private boolean deveAplicarPlanoNesteMes(MovimentoPlaneado plan, YearMonth mesAtual) {
        YearMonth inicio = YearMonth.from(plan.getDataInicio());
        YearMonth fim = plan.getDataFim() != null ? YearMonth.from(plan.getDataFim()) : YearMonth.now().plusYears(100);

        if (mesAtual.isBefore(inicio) || mesAtual.isAfter(fim)) {
            return false;
        }

        long mesesPassados = ChronoUnit.MONTHS.between(inicio, mesAtual);

        return switch (plan.getFrequencia()) {
            case MENSAL, SEMANAL -> true;
            case TRIMESTRAL -> mesesPassados % 3 == 0;
            case SEMESTRAL -> mesesPassados % 6 == 0;
            case ANUAL -> mesesPassados % 12 == 0;
            case PONTUAL -> mesesPassados == 0;
        };
    }

    // =========================================================================
    // --- 2. GESTÃO DE CONTAS BANCÁRIAS ---
    // =========================================================================

    @Transactional
    public ContaBancariaResponseDTO criarConta(ContaBancariaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));
        ContaBancaria c = new ContaBancaria();
        c.setNome(dto.getNome());
        c.setIban(dto.getIban());
        c.setSaldo(dto.getSaldoInicial() != null ? dto.getSaldoInicial() : BigDecimal.ZERO);
        c.setUtilizador(user);
        return converterContaParaDTO(contaRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ContaBancariaResponseDTO> listarContas() {
        return contaRepository.findAllByUtilizadorId(authService.getUtilizadorAutenticadoId())
                .stream().map(this::converterContaParaDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContaBancariaResponseDTO buscarContaPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));
        return converterContaParaDTO(conta);
    }

    // =========================================================================
    // --- 3. MOVIMENTOS E TRANSFERÊNCIAS ---
    // =========================================================================

    @Transactional
    public MovimentoResponseDTO registarMovimento(MovimentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new EntityNotFoundException("User não encontrado."));
        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(dto.getContaId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

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

    @Transactional
    public void transferirEntreContas(TransferenciaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new EntityNotFoundException("User não encontrado."));
        ContaBancaria origem = contaRepository.findByIdAndUtilizadorId(dto.getContaOrigemId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Origem não encontrada."));
        ContaBancaria destino = contaRepository.findByIdAndUtilizadorId(dto.getContaDestinoId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Destino não encontrado."));

        origem.setSaldo(origem.getSaldo().subtract(dto.getValor()));
        destino.setSaldo(destino.getSaldo().add(dto.getValor()));
        contaRepository.save(origem);
        contaRepository.save(destino);

        Movimento m1 = new Movimento();
        m1.setConta(origem); m1.setUtilizador(user); m1.setTipo(Movimento.TipoMovimento.DEBITO);
        m1.setValor(dto.getValor()); m1.setSaldoApos(origem.getSaldo()); m1.setDataMovimento(LocalDateTime.now());
        m1.setDescricao("Transferência para " + destino.getNome());

        Movimento m2 = new Movimento();
        m2.setConta(destino); m2.setUtilizador(user); m2.setTipo(Movimento.TipoMovimento.CREDITO);
        m2.setValor(dto.getValor()); m2.setSaldoApos(destino.getSaldo()); m2.setDataMovimento(LocalDateTime.now());
        m2.setDescricao("Transferência de " + origem.getNome());

        movimentoRepository.saveAll(List.of(m1, m2));
    }

    @Transactional(readOnly = true)
    public List<MovimentoResponseDTO> obterExtrato(Long contaId) {
        return movimentoRepository.findAllByContaIdOrderByDataMovimentoDesc(contaId)
                .stream().map(this::converterParaDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // --- 4. LIQUIDAÇÕES DE FATURAS ---
    // =========================================================================

    @Transactional(readOnly = true)
    public List<DocumentoPendenteDTO> listarPendentes() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        List<DocumentoPendenteDTO> pendentes = new ArrayList<>();
        List<EstadoPagamento> estadosIncompletos = List.of(EstadoPagamento.PENDENTE, EstadoPagamento.PARCIALMENTE_PAGO);

        vendaRepository.findAllByUtilizadorIdAndEstadoPagamentoIn(utilizadorId, estadosIncompletos).forEach(v -> {
            BigDecimal pendente = v.getTotalComIva().subtract(v.getValorPago());
            pendentes.add(new DocumentoPendenteDTO(v.getId(), "VENDA", v.getDataVenda(), v.getCliente().getNome(), v.getTotalComIva(), pendente));
        });

        compraRepository.findAllByUtilizadorIdAndEstadoPagamentoIn(utilizadorId, estadosIncompletos).forEach(c -> {
            BigDecimal pendente = c.getTotal().subtract(c.getValorPago());
            pendentes.add(new DocumentoPendenteDTO(c.getId(), "COMPRA", c.getDataCompra(), c.getFornecedor().getNome(), c.getTotal(), pendente));
        });

        pendentes.sort((a, b) -> a.getData().compareTo(b.getData()));
        return pendentes;
    }

    @Transactional
    public void confirmarTransacao(ConfirmarPagamentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId).orElseThrow(() -> new EntityNotFoundException("User não encontrado."));
        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(dto.getContaBancariaId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Conta não encontrada."));

        Movimento mov = new Movimento();
        mov.setConta(conta); mov.setUtilizador(user);
        mov.setDataMovimento(dto.getDataPagamento() != null ? dto.getDataPagamento() : LocalDateTime.now());
        BigDecimal valorPagamento = dto.getValorAPagar();

        if ("VENDA".equalsIgnoreCase(dto.getTipoDocumento())) {
            Venda venda = vendaRepository.findByIdAndUtilizadorId(dto.getDocumentoId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Venda não encontrada."));
            venda.setValorPago(venda.getValorPago().add(valorPagamento));
            venda.setEstadoPagamento(venda.getValorPago().compareTo(venda.getTotalComIva()) >= 0 ? EstadoPagamento.PAGO : EstadoPagamento.PARCIALMENTE_PAGO);
            vendaRepository.save(venda);
            mov.setTipo(Movimento.TipoMovimento.CREDITO); mov.setValor(valorPagamento);
            mov.setDescricao("Recebimento Venda #" + venda.getId() + " - " + venda.getCliente().getNome());
            mov.setVenda(venda); mov.setCliente(venda.getCliente());
            conta.setSaldo(conta.getSaldo().add(valorPagamento));
        } else {
            Compra compra = compraRepository.findByIdAndUtilizadorId(dto.getDocumentoId(), utilizadorId).orElseThrow(() -> new EntityNotFoundException("Compra não encontrada."));
            compra.setValorPago(compra.getValorPago().add(valorPagamento));
            compra.setEstadoPagamento(compra.getValorPago().compareTo(compra.getTotal()) >= 0 ? EstadoPagamento.PAGO : EstadoPagamento.PARCIALMENTE_PAGO);
            compraRepository.save(compra);
            mov.setTipo(Movimento.TipoMovimento.DEBITO); mov.setValor(valorPagamento);
            mov.setDescricao("Pagamento Compra #" + compra.getId() + " - " + compra.getFornecedor().getNome());
            mov.setCompra(compra); mov.setFornecedor(compra.getFornecedor());
            conta.setSaldo(conta.getSaldo().subtract(valorPagamento));
        }
        mov.setSaldoApos(conta.getSaldo());
        contaRepository.save(conta);
        movimentoRepository.save(mov);
    }

    @Transactional
    public void anularMovimento(Long movimentoId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Movimento mov = movimentoRepository.findByIdAndUtilizadorId(movimentoId, utilizadorId).orElseThrow(() -> new EntityNotFoundException("Movimento não encontrado."));
        ContaBancaria conta = mov.getConta();

        if (mov.getTipo() == Movimento.TipoMovimento.CREDITO) conta.setSaldo(conta.getSaldo().subtract(mov.getValor()));
        else conta.setSaldo(conta.getSaldo().add(mov.getValor()));
        contaRepository.save(conta);

        if (mov.getVenda() != null) {
            Venda v = mov.getVenda();
            v.setValorPago(v.getValorPago().subtract(mov.getValor()));
            v.setEstadoPagamento(v.getValorPago().compareTo(BigDecimal.ZERO) <= 0 ? EstadoPagamento.PENDENTE : EstadoPagamento.PARCIALMENTE_PAGO);
            vendaRepository.save(v);
        }
        if (mov.getCompra() != null) {
            Compra c = mov.getCompra();
            c.setValorPago(c.getValorPago().subtract(mov.getValor()));
            c.setEstadoPagamento(c.getValorPago().compareTo(BigDecimal.ZERO) <= 0 ? EstadoPagamento.PENDENTE : EstadoPagamento.PARCIALMENTE_PAGO);
            compraRepository.save(c);
        }
        movimentoRepository.delete(mov);
    }

    private ContaBancariaResponseDTO converterContaParaDTO(ContaBancaria c) {
        ContaBancariaResponseDTO dto = new ContaBancariaResponseDTO();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setIban(c.getIban());
        dto.setSaldo(c.getSaldo());
        return dto;
    }

    private MovimentoResponseDTO converterParaDTO(Movimento mov) {
        MovimentoResponseDTO dto = new MovimentoResponseDTO();
        dto.setId(mov.getId());
        dto.setDataMovimento(mov.getDataMovimento() != null ? mov.getDataMovimento().toString() : "");
        dto.setDescricao(mov.getDescricao());
        dto.setTipo(mov.getTipo().name());
        dto.setValor(mov.getValor());
        if (mov.getFornecedor() != null) dto.setFornecedorNome(mov.getFornecedor().getNome());
        if (mov.getCliente() != null) dto.setClienteNome(mov.getCliente().getNome());
        return dto;
    }
}