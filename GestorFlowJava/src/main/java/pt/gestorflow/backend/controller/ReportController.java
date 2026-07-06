package pt.gestorflow.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import pt.gestorflow.backend.dto.ContaCorrenteExtratoDTO;
import pt.gestorflow.backend.service.AnaliseService;
import pt.gestorflow.backend.service.PdfGeneratorService;
import pt.gestorflow.backend.service.ContaCorrenteService;
import pt.gestorflow.backend.repository.OrcamentoRepository;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.model.Orcamento;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.LinhaOrcamento;
import pt.gestorflow.backend.model.Empresa; // 🚀 Adicionado para a Empresa

import pt.gestorflow.backend.service.TesourariaService;
import pt.gestorflow.backend.service.PlaneamentoService;
import pt.gestorflow.backend.service.EmpresaService; // 🚀 Adicionado para a Empresa
import pt.gestorflow.backend.dto.DocumentoPendenteDTO;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.model.FrequenciaMovimento;
import pt.gestorflow.backend.model.TipoMovimentoPlaneado;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final PdfGeneratorService pdfGeneratorService;
    private final OrcamentoRepository orcamentoRepository;
    private final AnaliseService analiseService;
    private final ContaCorrenteService contaCorrenteService;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;
    private final TesourariaService tesourariaService;
    private final PlaneamentoService planeamentoService;
    private final EmpresaService empresaService; // 🚀 Nova Injeção do Cão de Guarda

    // 🚀 CONSTRUTOR ATUALIZADO
    public ReportController(PdfGeneratorService pdfGeneratorService,
                            OrcamentoRepository orcamentoRepository,
                            AnaliseService analiseService,
                            ContaCorrenteService contaCorrenteService,
                            ClienteRepository clienteRepository,
                            FornecedorRepository fornecedorRepository,
                            TesourariaService tesourariaService,
                            PlaneamentoService planeamentoService,
                            EmpresaService empresaService) {
        this.pdfGeneratorService = pdfGeneratorService;
        this.orcamentoRepository = orcamentoRepository;
        this.analiseService = analiseService;
        this.contaCorrenteService = contaCorrenteService;
        this.clienteRepository = clienteRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.tesourariaService = tesourariaService;
        this.planeamentoService = planeamentoService;
        this.empresaService = empresaService; // 🚀 Inicializado
    }

    private static class LinhaTimeline {
        LocalDate data;
        String descritivo;
        boolean isProjecao;
        java.math.BigDecimal receita;
        java.math.BigDecimal despesa;
        java.math.BigDecimal saldo;

        public LinhaTimeline(LocalDate data, String descritivo, boolean isProjecao, java.math.BigDecimal receita, java.math.BigDecimal despesa) {
            this.data = data;
            this.descritivo = descritivo;
            this.isProjecao = isProjecao;
            this.receita = receita;
            this.despesa = despesa;
        }
    }

    // ==========================================
    // ORÇAMENTOS
    // ==========================================
    @GetMapping("/orcamento/pdf/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadOrcamentoPdf(@PathVariable Long id) {

        // 🚀 1. BARREIRA DE SEGURANÇA
        Empresa empresa = empresaService.obterEmpresaValidada();

        Orcamento orcamentoEntity = orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orçamento não encontrado"));

        Map<String, Object> variables = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 🚀 2. INJETAR OS DADOS REAIS DA EMPRESA
        variables.put("empresaNome", empresa.getNomeFiscal());
        variables.put("empresaNif", empresa.getNif());
        variables.put("empresaMorada", empresa.getMoradaCompleta() != null ? empresa.getMoradaCompleta() : "");

        Map<String, String> orcamento = new HashMap<>();
        orcamento.put("numero", String.valueOf(orcamentoEntity.getId()));
        orcamento.put("dataEmissao", orcamentoEntity.getDataEmissao() != null ? orcamentoEntity.getDataEmissao().format(dateFormatter) : "");
        orcamento.put("dataValidade", orcamentoEntity.getDataValidade() != null ? orcamentoEntity.getDataValidade().format(dateFormatter) : "");
        orcamento.put("subTotalGeral", orcamentoEntity.getTotalSemIva() != null ? String.format("%.2f", orcamentoEntity.getTotalSemIva()) : "0,00");
        orcamento.put("totalGeral", orcamentoEntity.getTotalComIva() != null ? String.format("%.2f", orcamentoEntity.getTotalComIva()) : "0,00");
        orcamento.put("desconto", "0,00");
        orcamento.put("formaPagamento", "Pronto Pagamento / A Combinar");
        orcamento.put("observacoes", orcamentoEntity.getNotas() != null ? orcamentoEntity.getNotas() : "");
        variables.put("orcamento", orcamento);

        Cliente clienteEntity = orcamentoEntity.getCliente();
        Map<String, String> cliente = new HashMap<>();
        if (clienteEntity != null) {
            cliente.put("nome", clienteEntity.getNome() != null ? clienteEntity.getNome() : "");
            cliente.put("telefone", clienteEntity.getTelefone() != null ? clienteEntity.getTelefone() : "");
            cliente.put("email", clienteEntity.getEmail() != null ? clienteEntity.getEmail() : "");
            cliente.put("nif", clienteEntity.getNif() != null ? clienteEntity.getNif() : "");
            cliente.put("distrito", "-");
            cliente.put("endereco", "-");
            cliente.put("codigoPostal", "-");
            cliente.put("cidade", "-");
        } else {
            cliente.put("nome", "Consumidor Final");
            cliente.put("telefone", "-");
            cliente.put("email", "-");
            cliente.put("nif", "-");
            cliente.put("distrito", "-");
            cliente.put("endereco", "-");
            cliente.put("codigoPostal", "-");
            cliente.put("cidade", "-");
        }
        variables.put("cliente", cliente);

        List<Map<String, String>> itens = new ArrayList<>();
        if (orcamentoEntity.getLinhas() != null) {
            for (LinhaOrcamento linha : orcamentoEntity.getLinhas()) {
                Map<String, String> item = new HashMap<>();
                item.put("descricao", (linha.getArtigo() != null && linha.getArtigo().getNome() != null) ? linha.getArtigo().getNome() : "Artigo sem descrição");
                item.put("quantidade", linha.getQuantidade() != null ? linha.getQuantidade().toString() : "0");
                item.put("valorUnitario", linha.getPrecoVendaUnitario() != null ? String.format("%.2f", linha.getPrecoVendaUnitario()) : "0,00");
                item.put("subTotal", linha.getTotalLinhaComIva() != null ? String.format("%.2f", linha.getTotalLinhaComIva()) : "0,00");
                itens.add(item);
            }
        }
        variables.put("itens", itens);

        byte[] pdfBytes = pdfGeneratorService.generatePdf("orcamento-template", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Orcamento_" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================
    // DASHBOARD ANALÍTICO
    // ==========================================
    @GetMapping("/dashboard/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadDashboardPdf() {

        // 🚀 1. BARREIRA DE SEGURANÇA
        Empresa empresa = empresaService.obterEmpresaValidada();

        List<pt.gestorflow.backend.dto.AnaliseAnaliticaDTO> dados = analiseService.obterDashboard();

        Map<String, Object> variables = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
        variables.put("dataExtracao", java.time.LocalDateTime.now().format(dateFormatter));

        // 🚀 2. INJETAR OS DADOS REAIS DA EMPRESA
        variables.put("empresaNome", empresa.getNomeFiscal());
        variables.put("empresaNif", empresa.getNif());
        variables.put("empresaMorada", empresa.getMoradaCompleta() != null ? empresa.getMoradaCompleta() : "");

        java.math.BigDecimal totalGeralCompras = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalGeralVendas = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalGeralMargem = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalGeralIvaCompras = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalGeralIvaVendas = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalGeralSaldoIva = java.math.BigDecimal.ZERO;

        Map<String, List<pt.gestorflow.backend.dto.AnaliseAnaliticaDTO>> agrupados = new java.util.LinkedHashMap<>();
        for (pt.gestorflow.backend.dto.AnaliseAnaliticaDTO dto : dados) {
            String chave = dto.getCentroCustoCodigo() + "|" + dto.getCentroCustoNome();
            agrupados.computeIfAbsent(chave, k -> new ArrayList<>()).add(dto);

            totalGeralCompras = totalGeralCompras.add(dto.getTotalComprasSemIva() != null ? dto.getTotalComprasSemIva() : java.math.BigDecimal.ZERO);
            totalGeralVendas = totalGeralVendas.add(dto.getTotalVendasSemIva() != null ? dto.getTotalVendasSemIva() : java.math.BigDecimal.ZERO);
            totalGeralMargem = totalGeralMargem.add(dto.getMargemBruta() != null ? dto.getMargemBruta() : java.math.BigDecimal.ZERO);
            totalGeralIvaCompras = totalGeralIvaCompras.add(dto.getTotalIvaCompras() != null ? dto.getTotalIvaCompras() : java.math.BigDecimal.ZERO);
            totalGeralIvaVendas = totalGeralIvaVendas.add(dto.getTotalIvaVendas() != null ? dto.getTotalIvaVendas() : java.math.BigDecimal.ZERO);
            totalGeralSaldoIva = totalGeralSaldoIva.add(dto.getSaldoIva() != null ? dto.getSaldoIva() : java.math.BigDecimal.ZERO);
        }

        List<Map<String, Object>> gruposParaPdf = new ArrayList<>();

        for (Map.Entry<String, List<pt.gestorflow.backend.dto.AnaliseAnaliticaDTO>> entry : agrupados.entrySet()) {
            Map<String, Object> grupo = new HashMap<>();
            String[] split = entry.getKey().split("\\|");
            grupo.put("centroCodigo", split[0]);
            grupo.put("centroNome", split.length > 1 ? split[1] : "");

            List<Map<String, String>> linhasMapeadas = new ArrayList<>();
            java.math.BigDecimal subCompras = java.math.BigDecimal.ZERO;
            java.math.BigDecimal subVendas = java.math.BigDecimal.ZERO;
            java.math.BigDecimal subMargem = java.math.BigDecimal.ZERO;
            java.math.BigDecimal subIvaCompras = java.math.BigDecimal.ZERO;
            java.math.BigDecimal subIvaVendas = java.math.BigDecimal.ZERO;
            java.math.BigDecimal subSaldoIva = java.math.BigDecimal.ZERO;

            for (pt.gestorflow.backend.dto.AnaliseAnaliticaDTO l : entry.getValue()) {
                Map<String, String> linha = new HashMap<>();
                linha.put("seccaoCodigo", l.getSeccaoCodigo() != null ? l.getSeccaoCodigo() : "");
                linha.put("seccaoNome", l.getSeccaoNome() != null ? l.getSeccaoNome() : "Geral");
                linha.put("compras", String.format("%.2f €", l.getTotalComprasSemIva()));
                linha.put("vendas", String.format("%.2f €", l.getTotalVendasSemIva()));
                linha.put("margem", String.format("%.2f €", l.getMargemBruta()));
                linha.put("ivaCompras", String.format("%.2f €", l.getTotalIvaCompras()));
                linha.put("ivaVendas", String.format("%.2f €", l.getTotalIvaVendas()));
                linha.put("saldoIva", String.format("%.2f €", l.getSaldoIva()));

                subCompras = subCompras.add(l.getTotalComprasSemIva() != null ? l.getTotalComprasSemIva() : java.math.BigDecimal.ZERO);
                subVendas = subVendas.add(l.getTotalVendasSemIva() != null ? l.getTotalVendasSemIva() : java.math.BigDecimal.ZERO);
                subMargem = subMargem.add(l.getMargemBruta() != null ? l.getMargemBruta() : java.math.BigDecimal.ZERO);
                subIvaCompras = subIvaCompras.add(l.getTotalIvaCompras() != null ? l.getTotalIvaCompras() : java.math.BigDecimal.ZERO);
                subIvaVendas = subIvaVendas.add(l.getTotalIvaVendas() != null ? l.getTotalIvaVendas() : java.math.BigDecimal.ZERO);
                subSaldoIva = subSaldoIva.add(l.getSaldoIva() != null ? l.getSaldoIva() : java.math.BigDecimal.ZERO);

                linhasMapeadas.add(linha);
            }

            grupo.put("linhas", linhasMapeadas);
            grupo.put("subCompras", String.format("%.2f €", subCompras));
            grupo.put("subVendas", String.format("%.2f €", subVendas));
            grupo.put("subMargem", String.format("%.2f €", subMargem));
            grupo.put("subIvaCompras", String.format("%.2f €", subIvaCompras));
            grupo.put("subIvaVendas", String.format("%.2f €", subIvaVendas));
            grupo.put("subSaldoIva", String.format("%.2f €", subSaldoIva));

            gruposParaPdf.add(grupo);
        }

        variables.put("grupos", gruposParaPdf);

        Map<String, String> totais = new HashMap<>();
        totais.put("compras", String.format("%.2f €", totalGeralCompras));
        totais.put("vendas", String.format("%.2f €", totalGeralVendas));
        totais.put("margem", String.format("%.2f €", totalGeralMargem));
        totais.put("ivaCompras", String.format("%.2f €", totalGeralIvaCompras));
        totais.put("ivaVendas", String.format("%.2f €", totalGeralIvaVendas));
        totais.put("saldoIva", String.format("%.2f €", totalGeralSaldoIva));
        variables.put("totais", totais);

        byte[] pdfBytes = pdfGeneratorService.generatePdf("dashboard-template", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "GestorFlow_Dashboard.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================
    // EXTRATO CONTA CORRENTE - CLIENTES
    // ==========================================
    @GetMapping("/conta-corrente/cliente/pdf/{clienteId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadExtratoClientePdf(@PathVariable Long clienteId) {

        // 🚀 1. BARREIRA DE SEGURANÇA
        Empresa empresa = empresaService.obterEmpresaValidada();

        List<ContaCorrenteExtratoDTO> extrato = contaCorrenteService.obterExtratoCliente(clienteId);

        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        String nomeCliente = cliente != null ? cliente.getNome() : "Cliente #" + clienteId;

        Map<String, Object> variables = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        variables.put("nomeCliente", nomeCliente);
        variables.put("dataExtracao", java.time.LocalDate.now().format(dateFormatter));

        // 🚀 2. INJETAR OS DADOS REAIS DA EMPRESA
        variables.put("empresaNome", empresa.getNomeFiscal());
        variables.put("empresaNif", empresa.getNif());
        variables.put("empresaMorada", empresa.getMoradaCompleta() != null ? empresa.getMoradaCompleta() : "");

        java.math.BigDecimal saldoFinal = java.math.BigDecimal.ZERO;
        if (!extrato.isEmpty()) {
            saldoFinal = extrato.get(extrato.size() - 1).getSaldoAcumulado();
        }
        variables.put("saldoFinal", String.format("%.2f €", saldoFinal));

        List<Map<String, String>> linhasPdf = new ArrayList<>();
        for (ContaCorrenteExtratoDTO linha : extrato) {
            Map<String, String> item = new HashMap<>();

            item.put("data", linha.getDataMovimento() != null ? linha.getDataMovimento().format(dateFormatter) : "-");
            item.put("descricao", linha.getDescricao() != null ? linha.getDescricao() : "");
            item.put("tipo", linha.getTipoDocumento() != null ? linha.getTipoDocumento() : "");

            boolean temDebito = linha.getDebito() != null && linha.getDebito().compareTo(java.math.BigDecimal.ZERO) > 0;
            boolean temCredito = linha.getCredito() != null && linha.getCredito().compareTo(java.math.BigDecimal.ZERO) > 0;

            item.put("debito", temDebito ? String.format("%.2f €", linha.getDebito()) : "-");
            item.put("credito", temCredito ? String.format("%.2f €", linha.getCredito()) : "-");
            item.put("saldo", linha.getSaldoAcumulado() != null ? String.format("%.2f €", linha.getSaldoAcumulado()) : "0,00 €");

            linhasPdf.add(item);
        }
        variables.put("linhas", linhasPdf);

        byte[] pdfBytes = pdfGeneratorService.generatePdf("conta-corrente-template", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Extrato_" + nomeCliente.replace(" ", "_") + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================
    // EXTRATO CONTA CORRENTE - FORNECEDORES
    // ==========================================
    @GetMapping("/conta-corrente/fornecedor/pdf/{fornecedorId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadExtratoFornecedorPdf(@PathVariable Long fornecedorId) {

        // 🚀 1. BARREIRA DE SEGURANÇA
        Empresa empresa = empresaService.obterEmpresaValidada();

        List<ContaCorrenteExtratoDTO> extrato = contaCorrenteService.obterExtratoFornecedor(fornecedorId);

        var fornecedorOpt = fornecedorRepository.findById(fornecedorId);
        String nomeEntidade = fornecedorOpt.isPresent() ? fornecedorOpt.get().getNome() : "Fornecedor #" + fornecedorId;

        Map<String, Object> variables = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        variables.put("nomeCliente", nomeEntidade);
        variables.put("dataExtracao", java.time.LocalDate.now().format(dateFormatter));

        // 🚀 2. INJETAR OS DADOS REAIS DA EMPRESA
        variables.put("empresaNome", empresa.getNomeFiscal());
        variables.put("empresaNif", empresa.getNif());
        variables.put("empresaMorada", empresa.getMoradaCompleta() != null ? empresa.getMoradaCompleta() : "");

        java.math.BigDecimal saldoFinal = java.math.BigDecimal.ZERO;
        if (!extrato.isEmpty()) {
            saldoFinal = extrato.get(extrato.size() - 1).getSaldoAcumulado();
        }
        variables.put("saldoFinal", String.format("%.2f €", saldoFinal));

        List<Map<String, String>> linhasPdf = new ArrayList<>();
        for (ContaCorrenteExtratoDTO linha : extrato) {
            Map<String, String> item = new HashMap<>();

            item.put("data", linha.getDataMovimento() != null ? linha.getDataMovimento().format(dateFormatter) : "-");
            item.put("descricao", linha.getDescricao() != null ? linha.getDescricao() : "");
            item.put("tipo", linha.getTipoDocumento() != null ? linha.getTipoDocumento() : "");

            boolean temDebito = linha.getDebito() != null && linha.getDebito().compareTo(java.math.BigDecimal.ZERO) > 0;
            boolean temCredito = linha.getCredito() != null && linha.getCredito().compareTo(java.math.BigDecimal.ZERO) > 0;

            item.put("debito", temDebito ? String.format("%.2f €", linha.getDebito()) : "-");
            item.put("credito", temCredito ? String.format("%.2f €", linha.getCredito()) : "-");
            item.put("saldo", linha.getSaldoAcumulado() != null ? String.format("%.2f €", linha.getSaldoAcumulado()) : "0,00 €");

            linhasPdf.add(item);
        }
        variables.put("linhas", linhasPdf);

        byte[] pdfBytes = pdfGeneratorService.generatePdf("conta-corrente-template", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Extrato_Fornecedor_" + nomeEntidade.replace(" ", "_") + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // ==========================================
    // EVOLUÇÃO DE SALDO (TESOURARIA)
    // ==========================================
    @GetMapping("/tesouraria/evolucao/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadEvolucaoSaldoPdf(
            @RequestParam(defaultValue = "TUDO") String fluxo,
            @RequestParam(defaultValue = "TUDO") String natureza,
            @RequestParam(defaultValue = "TUDO") String periodo) {

        // 🚀 1. BARREIRA DE SEGURANÇA
        Empresa empresa = empresaService.obterEmpresaValidada();

        List<LinhaTimeline> timeline = new ArrayList<>();

        for (DocumentoPendenteDTO doc : tesourariaService.listarPendentes()) {
            boolean isReceita = "VENDA".equals(doc.getTipo()) || "RECEITA".equals(doc.getTipo());
            String descricao = doc.getEntidade() + (doc.getDescricao() != null ? " - " + doc.getDescricao() : "");
            timeline.add(new LinhaTimeline(doc.getData(), descricao, false,
                    isReceita ? doc.getValorPendente() : null,
                    !isReceita ? doc.getValorPendente() : null));
        }

        LocalDate limiteMaximo = LocalDate.now().plusYears(10);
        for (MovimentoPlaneadoDTO plano : planeamentoService.listarPlanos()) {
            if (plano.getAtivo() != null && !plano.getAtivo()) continue;

            LocalDate dataCursor = plano.getDataInicio();
            LocalDate dataFim = plano.getDataFim() != null ? plano.getDataFim() : limiteMaximo;
            if (dataFim.isAfter(limiteMaximo)) dataFim = limiteMaximo;

            while (!dataCursor.isAfter(dataFim)) {
                final LocalDate cursorFinal = dataCursor;
                boolean ignorado = plano.getDatasIgnoradas() != null && plano.getDatasIgnoradas().stream().anyMatch(d -> d.equals(cursorFinal));

                if (!ignorado) {
                    boolean isEntrada = TipoMovimentoPlaneado.ENTRADA.equals(plano.getTipo());
                    timeline.add(new LinhaTimeline(dataCursor, plano.getDescricao() + " (Previsão)", true,
                            isEntrada ? plano.getValorBase() : null,
                            !isEntrada ? plano.getValorBase() : null));
                }

                if (plano.getFrequencia() == FrequenciaMovimento.PONTUAL) break;

                switch (plano.getFrequencia()) {
                    case SEMANAL -> dataCursor = dataCursor.plusWeeks(1);
                    case MENSAL -> dataCursor = dataCursor.plusMonths(1);
                    case TRIMESTRAL -> dataCursor = dataCursor.plusMonths(3);
                    case SEMESTRAL -> dataCursor = dataCursor.plusMonths(6);
                    case ANUAL -> dataCursor = dataCursor.plusYears(1);
                    default -> dataCursor = dataCursor.plusDays(1);
                }
            }
        }

        timeline.sort(Comparator.comparing(l -> l.data));

        pt.gestorflow.backend.dto.SimuladorTesourariaDTO simulacao = tesourariaService.obterSimulacao();
        java.math.BigDecimal saldoAcumulado = simulacao.getSaldoAtual();

        LocalDate fimEsteMes = YearMonth.now().atEndOfMonth();
        LocalDate fimTresMeses = YearMonth.now().plusMonths(2).atEndOfMonth();

        List<Map<String, String>> linhasPdf = new ArrayList<>();

        for (LinhaTimeline linha : timeline) {
            if (linha.receita != null) saldoAcumulado = saldoAcumulado.add(linha.receita);
            if (linha.despesa != null) saldoAcumulado = saldoAcumulado.subtract(linha.despesa);
            linha.saldo = saldoAcumulado;

            if ("ENTRADAS".equals(fluxo) && linha.receita == null) continue;
            if ("SAIDAS".equals(fluxo) && linha.despesa == null) continue;

            if ("REAIS".equals(natureza) && linha.isProjecao) continue;
            if ("PLANOS".equals(natureza) && !linha.isProjecao) continue;

            if ("ESTE_MES".equals(periodo) && linha.data.isAfter(fimEsteMes)) continue;
            if ("TRES_MESES".equals(periodo) && linha.data.isAfter(fimTresMeses)) continue;

            Map<String, String> item = new HashMap<>();
            item.put("data", linha.data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            item.put("descricao", linha.descritivo);
            item.put("debito", linha.despesa != null ? String.format("%.2f €", linha.despesa) : "-");
            item.put("credito", linha.receita != null ? String.format("%.2f €", linha.receita) : "-");
            item.put("saldo", String.format("%.2f €", linha.saldo));
            item.put("isProjecao", String.valueOf(linha.isProjecao));

            linhasPdf.add(item);
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("dataExtracao", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        variables.put("saldoInicial", String.format("%.2f €", simulacao.getSaldoAtual()));
        variables.put("saldoFinal", String.format("%.2f €", saldoAcumulado));
        variables.put("linhas", linhasPdf);
        variables.put("filtroLabel", String.format("Filtros Ativos: Fluxo [%s] | Origem [%s] | Período [%s]", fluxo, natureza, periodo));

        // 🚀 2. INJETAR OS DADOS REAIS DA EMPRESA
        variables.put("empresaNome", empresa.getNomeFiscal());
        variables.put("empresaNif", empresa.getNif());
        variables.put("empresaMorada", empresa.getMoradaCompleta() != null ? empresa.getMoradaCompleta() : "");

        byte[] pdfBytes = pdfGeneratorService.generatePdf("evolucao-saldo-template", variables);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Evolucao_Saldo.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}