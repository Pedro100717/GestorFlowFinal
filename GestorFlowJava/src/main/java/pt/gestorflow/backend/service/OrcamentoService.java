package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.OrcamentoResponseDTO;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ArtigoRepository artigoRepository;
    private final TxIvaRepository txIvaRepository;
    private final VendaService vendaService;

    // 🚀 Injeções de Segurança
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    // --- 1. CRIAR ---
    @Transactional
    public OrcamentoResponseDTO criarOrcamento(OrcamentoDTO dto) {
        // 🚀 1. ID Blindado
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 2. Entidade do Utilizador para associar ao Orçamento
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        // 🛡️ PROTEÇÃO IDOR: Garante que o cliente é da própria empresa
        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setUtilizador(user);
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setNotas(dto.getNotas());
        orcamento.setEstado(Orcamento.EstadoOrcamento.RASCUNHO);

        // 🛡️ Passamos o User ID para garantir a segurança nas linhas
        processarLinhasOrcamento(orcamento, dto, utilizadorId);

        return converterParaDTO(orcamentoRepository.save(orcamento));
    }

    // --- 2. ATUALIZAR ---
    @Transactional
    public OrcamentoResponseDTO atualizarOrcamento(Long id, OrcamentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado ou sem permissão."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Não é possível alterar um orçamento que já foi convertido em venda.");
        }

        if (dto.getClienteId() != null) {
            Cliente novoCliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));
            orcamento.setCliente(novoCliente);
        }
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setNotas(dto.getNotas());

        orcamento.getLinhas().clear();
        processarLinhasOrcamento(orcamento, dto, utilizadorId);

        return converterParaDTO(orcamentoRepository.save(orcamento));
    }

    // 🛡️ CORREÇÃO IDOR: Adicionado o `Long userId` para validar os artigos das linhas
    private void processarLinhasOrcamento(Orcamento orcamento, OrcamentoDTO dto, Long userId) {
        BigDecimal totalCustoGeral = BigDecimal.ZERO;
        BigDecimal totalSemIvaGeral = BigDecimal.ZERO;
        BigDecimal totalComIvaGeral = BigDecimal.ZERO;

        for (OrcamentoDTO.LinhaOrcamentoDTO linhaDto : dto.getLinhas()) {

            // 🛡️ CORREÇÃO CRÍTICA: Bloquear injeção de IDs de artigos da concorrência
            Artigo artigo = artigoRepository.findByIdAndUtilizadorId(linhaDto.getArtigoId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado: ID " + linhaDto.getArtigoId()));

            TxIva taxaIva = txIvaRepository.findById(linhaDto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA não encontrada"));

            LinhaOrcamento linha = new LinhaOrcamento();
            linha.setOrcamento(orcamento);
            linha.setArtigo(artigo);
            linha.setTaxaIva(taxaIva);
            linha.setQuantidade(linhaDto.getQuantidade());

            BigDecimal custoUnitario = artigo.getUltimoPrecoCusto() != null ? artigo.getUltimoPrecoCusto() : BigDecimal.ZERO;
            linha.setPrecoCustoUnitario(custoUnitario);

            BigDecimal precoVendaFinal;
            if (linhaDto.getPrecoVendaUnitarioOverride() != null) {
                precoVendaFinal = linhaDto.getPrecoVendaUnitarioOverride();
                if (custoUnitario.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lucro = precoVendaFinal.subtract(custoUnitario);
                    linha.setMargemLucroPercentual(lucro.divide(custoUnitario, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
                } else {
                    linha.setMargemLucroPercentual(BigDecimal.valueOf(100));
                }
            } else {
                BigDecimal margem = linhaDto.getMargemLucroPercentual() != null ? linhaDto.getMargemLucroPercentual() : BigDecimal.ZERO;
                linha.setMargemLucroPercentual(margem);
                BigDecimal fatorMargem = margem.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).add(BigDecimal.ONE);
                precoVendaFinal = custoUnitario.multiply(fatorMargem);
            }
            linha.setPrecoVendaUnitario(precoVendaFinal);

            BigDecimal totalLinhaSemIva = precoVendaFinal.multiply(linha.getQuantidade());
            BigDecimal fatorIva = taxaIva.getValor().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).add(BigDecimal.ONE);
            BigDecimal totalLinhaComIva = totalLinhaSemIva.multiply(fatorIva);

            linha.setTotalLinhaSemIva(totalLinhaSemIva);
            linha.setTotalLinhaComIva(totalLinhaComIva);

            orcamento.getLinhas().add(linha);

            totalCustoGeral = totalCustoGeral.add(custoUnitario.multiply(linha.getQuantidade()));
            totalSemIvaGeral = totalSemIvaGeral.add(totalLinhaSemIva);
            totalComIvaGeral = totalComIvaGeral.add(totalLinhaComIva);
        }

        orcamento.setTotalCusto(totalCustoGeral);
        orcamento.setTotalSemIva(totalSemIvaGeral);
        orcamento.setTotalComIva(totalComIvaGeral);
    }

    // --- 3. CONVERTER EM VENDA ---
    @Transactional
    public void converterEmVenda(Long orcamentoId, Long contaBancariaId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(orcamentoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Este orçamento já foi processado anteriormente.");
        }

        // 🛡️ 1. Criar o Cabeçalho da Venda (UM único documento)
        VendaDTO vendaDTO = new VendaDTO();
        vendaDTO.setClienteId(orcamento.getCliente().getId());
        vendaDTO.setDataVenda(LocalDateTime.now().toLocalDate());

        // 🛡️ 2. Preparar a lista de linhas
        List<VendaDTO.LinhaVendaDTO> linhasVenda = new ArrayList<>();

        // 🛡️ 3. Transferir as Linhas do Orçamento para as Linhas da Venda
        for (LinhaOrcamento linha : orcamento.getLinhas()) {
            VendaDTO.LinhaVendaDTO linhaDTO = new VendaDTO.LinhaVendaDTO();
            linhaDTO.setArtigoId(linha.getArtigo().getId());
            linhaDTO.setTaxaIvaId(linha.getTaxaIva().getId());
            linhaDTO.setQuantidade(linha.getQuantidade());
            linhaDTO.setPrecoUnitario(linha.getPrecoVendaUnitario());
            linhaDTO.setDesignacaoPersonalizada("Origem: Orçamento #" + orcamento.getId());

            linhasVenda.add(linhaDTO);
        }

        // 🛡️ 4. Anexar as linhas ao cabeçalho da Venda
        vendaDTO.setLinhas(linhasVenda);

        // 🛡️ 5. Registar a Venda
        vendaService.registarVenda(vendaDTO);

        // 🛡️ 6. Fechar o Orçamento
        orcamento.setEstado(Orcamento.EstadoOrcamento.CONVERTIDO_VENDA);
        orcamentoRepository.save(orcamento);
    }

    // --- 4. LISTAR E BUSCAR ---
    @Transactional(readOnly = true)
    public Page<OrcamentoResponseDTO> listarMeusOrcamentos(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataCriacaoSistema").descending());
        return orcamentoRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public OrcamentoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado ou sem permissão."));
        return converterParaDTO(orcamento);
    }

    // --- 5. ELIMINAR ---
    @Transactional
    public void eliminarOrcamento(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Não é possível eliminar um orçamento que já gerou vendas. Arquive-o ou anule a venda primeiro.");
        }
        orcamentoRepository.delete(orcamento);
    }

    // --- 6. ALTERAR ESTADO ---
    @Transactional
    public OrcamentoResponseDTO alterarEstado(Long id, Orcamento.EstadoOrcamento novoEstado) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            throw new RuntimeException("Orçamento já convertido em venda.");
        }
        orcamento.setEstado(novoEstado);
        return converterParaDTO(orcamentoRepository.save(orcamento));
    }

    // --- 7. CONVERSOR DTO ---
    private OrcamentoResponseDTO converterParaDTO(Orcamento o) {
        OrcamentoResponseDTO dto = new OrcamentoResponseDTO();
        dto.setId(o.getId());
        dto.setDataCriacao(o.getDataCriacaoSistema());
        dto.setDataValidade(o.getDataValidade());
        dto.setEstado(o.getEstado().name());
        dto.setNotas(o.getNotas());
        dto.setTotalCusto(o.getTotalCusto());
        dto.setTotalSemIva(o.getTotalSemIva());
        dto.setTotalComIva(o.getTotalComIva());

        if (o.getCliente() != null) {
            dto.setClienteId(o.getCliente().getId());
            dto.setClienteNome(o.getCliente().getNome());
        }

        if (o.getLinhas() != null) {
            List<OrcamentoResponseDTO.LinhaResponseDTO> linhasDto = o.getLinhas().stream().map(linha -> {
                OrcamentoResponseDTO.LinhaResponseDTO lDto = new OrcamentoResponseDTO.LinhaResponseDTO();
                lDto.setId(linha.getId());
                if (linha.getArtigo() != null) {
                    lDto.setArtigoId(linha.getArtigo().getId());
                    lDto.setArtigoNome(linha.getArtigo().getNome());
                }
                lDto.setQuantidade(linha.getQuantidade());
                lDto.setPrecoCustoUnitario(linha.getPrecoCustoUnitario());
                lDto.setPrecoVendaUnitario(linha.getPrecoVendaUnitario());
                lDto.setMargemLucroPercentual(linha.getMargemLucroPercentual());
                lDto.setTotalLinhaSemIva(linha.getTotalLinhaSemIva());
                lDto.setTotalLinhaComIva(linha.getTotalLinhaComIva());

                if (linha.getTaxaIva() != null) {
                    lDto.setTaxaIvaId(linha.getTaxaIva().getId());
                    lDto.setTaxaIvaValor(linha.getTaxaIva().getValor());
                }
                return lDto;
            }).collect(Collectors.toList());
            dto.setLinhas(linhasDto);
        }
        return dto;
    }
}