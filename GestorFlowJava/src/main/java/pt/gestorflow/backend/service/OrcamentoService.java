package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ArtigoRepository artigoRepository;
    private final TxIvaRepository txIvaRepository;
    private final VendaService vendaService; // Para converter em venda

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // --- 1. CRIAR ---
    @Transactional
    public Orcamento criarOrcamento(OrcamentoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setUtilizador(user);
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setNotas(dto.getNotas());
        orcamento.setEstado(Orcamento.EstadoOrcamento.RASCUNHO); // Começa sempre como Rascunho

        // Processa as linhas e calcula totais
        processarLinhasOrcamento(orcamento, dto);

        return orcamentoRepository.save(orcamento);
    }

    // --- 2. ATUALIZAR (CRÍTICO PARA O "SIMULADOR") ---
    @Transactional
    public Orcamento atualizarOrcamento(Long id, OrcamentoDTO dto) {
        Orcamento orcamento = buscarPorId(id); // Já valida utilizador

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Não é possível alterar um orçamento que já foi convertido em venda.");
        }

        // Atualiza cabeçalho
        if (dto.getClienteId() != null) {
            Cliente novoCliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), getUtilizadorLogado().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));
            orcamento.setCliente(novoCliente);
        }
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setNotas(dto.getNotas());

        // Atualiza estado se vier no DTO (opcional, ou via endpoint específico)
        // orcamento.setEstado(dto.getEstado());

        // Limpa as linhas antigas e adiciona as novas (Estratégia mais limpa para edição completa)
        orcamento.getLinhas().clear();
        processarLinhasOrcamento(orcamento, dto);

        return orcamentoRepository.save(orcamento);
    }

    // --- Lógica Auxiliar de Cálculo (Usada no Criar e Atualizar) ---
    private void processarLinhasOrcamento(Orcamento orcamento, OrcamentoDTO dto) {
        BigDecimal totalCustoGeral = BigDecimal.ZERO;
        BigDecimal totalSemIvaGeral = BigDecimal.ZERO;
        BigDecimal totalComIvaGeral = BigDecimal.ZERO;

        for (OrcamentoDTO.LinhaOrcamentoDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = artigoRepository.findById(linhaDto.getArtigoId())
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado: ID " + linhaDto.getArtigoId()));
            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            LinhaOrcamento linha = new LinhaOrcamento();
            linha.setOrcamento(orcamento);
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());

            // 1. Custo (Snapshot)
            BigDecimal custoUnitario = artigo.getUltimoPrecoCusto() != null ? artigo.getUltimoPrecoCusto() : BigDecimal.ZERO;
            linha.setPrecoCustoUnitario(custoUnitario);

            // 2. Cálculo do Preço de Venda (Margem vs Override)
            BigDecimal precoVendaFinal;
            if (linhaDto.getPrecoVendaUnitarioOverride() != null) {
                // Utilizador forçou um preço final
                precoVendaFinal = linhaDto.getPrecoVendaUnitarioOverride();
                // Calcula a margem implícita para registro
                if (custoUnitario.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lucro = precoVendaFinal.subtract(custoUnitario);
                    linha.setMargemLucroPercentual(lucro.divide(custoUnitario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                } else {
                    linha.setMargemLucroPercentual(BigDecimal.valueOf(100));
                }
            } else {
                // Utilizador definiu uma margem %
                BigDecimal margem = linhaDto.getMargemLucroPercentual() != null ? linhaDto.getMargemLucroPercentual() : BigDecimal.ZERO;
                linha.setMargemLucroPercentual(margem);
                BigDecimal fatorMargem = margem.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).add(BigDecimal.ONE);
                precoVendaFinal = custoUnitario.multiply(fatorMargem);
            }
            linha.setPrecoVendaUnitario(precoVendaFinal);

            // 3. Totais da Linha
            BigDecimal totalLinhaSemIva = precoVendaFinal.multiply(linha.getQuantidade());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva);

            linha.setTotalLinhaSemIva(totalLinhaSemIva);
            linha.setTotalLinhaComIva(totalLinhaComIva);

            // Adiciona à lista
            orcamento.getLinhas().add(linha);

            // Acumula Totais Gerais
            totalCustoGeral = totalCustoGeral.add(custoUnitario.multiply(linha.getQuantidade()));
            totalSemIvaGeral = totalSemIvaGeral.add(totalLinhaSemIva);
            totalComIvaGeral = totalComIvaGeral.add(totalLinhaComIva);
        }

        orcamento.setTotalCusto(totalCustoGeral);
        orcamento.setTotalSemIva(totalSemIvaGeral);
        orcamento.setTotalComIva(totalComIvaGeral);
    }

    // --- 3. CONVERTER EM VENDA (A "Magia") ---
    @Transactional
    public void converterEmVenda(Long orcamentoId, Long contaBancariaId) { // <--- RECEBE A CONTA
        Orcamento orcamento = buscarPorId(orcamentoId);

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Este orçamento já foi processado anteriormente.");
        }

        for (LinhaOrcamento linha : orcamento.getLinhas()) {
            VendaDTO vendaDTO = new VendaDTO();
            vendaDTO.setClienteId(orcamento.getCliente().getId());
            vendaDTO.setArtigoId(linha.getArtigo().getId());
            vendaDTO.setTaxaIvaId(linha.getTaxaIva().getId());
            vendaDTO.setQuantidade(linha.getQuantidade());
            vendaDTO.setPrecoUnitario(linha.getPrecoVendaUnitario());
            vendaDTO.setDesignacaoPersonalizada("Origem: Orçamento #" + orcamento.getId());
            vendaDTO.setDataVenda(LocalDateTime.now().toLocalDate());

            // ---> A MÁGICA AQUI: Associar o dinheiro à conta escolhida! <---
            vendaDTO.setContaBancariaId(contaBancariaId);

            vendaService.registarVenda(vendaDTO);
        }

        orcamento.setEstado(Orcamento.EstadoOrcamento.CONVERTIDO_VENDA);
        orcamentoRepository.save(orcamento);
    }

    // --- 4. LISTAR E BUSCAR ---
    public org.springframework.data.domain.Page<Orcamento> listarMeusOrcamentos(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(pagina, tamanho, org.springframework.data.domain.Sort.by("dataCriacao").descending());
        return orcamentoRepository.findAllByUtilizadorId(user.getId(), pageable);
    }

    public Orcamento buscarPorId(Long id) {
        return orcamentoRepository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado ou sem permissão."));
    }

    // --- 5. ELIMINAR ---
    @Transactional
    public void eliminarOrcamento(Long id) {
        Orcamento orcamento = buscarPorId(id);
        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Não é possível eliminar um orçamento que já gerou vendas. Arquive-o ou anule a venda primeiro.");
        }
        orcamentoRepository.delete(orcamento);
    }

    // --- 6. ALTERAR ESTADO (Aprovado/Rejeitado) ---
    public Orcamento alterarEstado(Long id, Orcamento.EstadoOrcamento novoEstado) {
        Orcamento orcamento = buscarPorId(id);
        // Não permitir voltar atrás se já foi convertido
        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Orçamento já convertido em venda.");
        }
        orcamento.setEstado(novoEstado);
        return orcamentoRepository.save(orcamento);
    }
}