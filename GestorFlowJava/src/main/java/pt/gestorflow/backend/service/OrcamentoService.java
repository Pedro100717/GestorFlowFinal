package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
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
import pt.gestorflow.backend.dto.LinhaVendaDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ArtigoRepository artigoRepository;
    private final TxIvaRepository txIvaRepository;
    private final VendaService vendaService;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public OrcamentoResponseDTO criarOrcamento(OrcamentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de Orçamento para o cliente ID: {} (Utilizador: {})", dto.getClienteId(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setUtilizador(user);
        orcamento.setDataValidade(dto.getDataValidade());
        orcamento.setNotas(dto.getNotas());
        orcamento.setEstado(Orcamento.EstadoOrcamento.RASCUNHO);

        processarLinhasOrcamento(orcamento, dto, utilizadorId);

        Orcamento salvo = orcamentoRepository.save(orcamento);
        log.debug("Orçamento ID: {} criado com sucesso no valor total de: {}", salvo.getId(), salvo.getTotalComIva());

        return converterParaDTO(salvo);
    }

    @Transactional
    public OrcamentoResponseDTO atualizarOrcamento(Long id, OrcamentoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Orçamento ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado ou sem permissão."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            log.warn("Tentativa de edição bloqueada: Orçamento ID: {} já convertido em venda.", id);
            throw new IllegalArgumentException("Não é possível alterar um orçamento que já foi convertido em venda.");
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

        Orcamento atualizado = orcamentoRepository.save(orcamento);
        log.debug("Orçamento ID: {} atualizado com sucesso.", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    private void processarLinhasOrcamento(Orcamento orcamento, OrcamentoDTO dto, Long userId) {
        BigDecimal totalCustoGeral = BigDecimal.ZERO;
        BigDecimal totalSemIvaGeral = BigDecimal.ZERO;
        BigDecimal totalComIvaGeral = BigDecimal.ZERO;

        Map<Long, Artigo> mapaArtigos = carregarArtigos(dto.getLinhas(), userId);
        Map<Long, TxIva> mapaIvas = carregarIvas(dto.getLinhas());

        for (OrcamentoDTO.LinhaOrcamentoDTO linhaDto : dto.getLinhas()) {
            Artigo artigo = mapaArtigos.get(linhaDto.getArtigoId());
            if (artigo == null) throw new EntityNotFoundException("Artigo não encontrado: " + linhaDto.getArtigoId());

            TxIva taxaIva = mapaIvas.get(linhaDto.getTaxaIvaId());
            if (taxaIva == null) throw new EntityNotFoundException("Taxa de IVA não encontrada para a linha.");

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

    @Transactional
    public void converterEmVenda(Long orcamentoId, Long contaBancariaId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Ação Financeira: O utilizador ID: {} pediu a conversão do Orçamento ID: {} em Venda", utilizadorId, orcamentoId);

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(orcamentoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            log.warn("Tentativa de conversão dupla bloqueada no Orçamento ID: {}", orcamentoId);
            throw new IllegalArgumentException("Este orçamento já foi processado anteriormente.");
        }

        VendaDTO vendaDTO = new VendaDTO();
        vendaDTO.setClienteId(orcamento.getCliente().getId());
        vendaDTO.setDataVenda(LocalDate.now());
        vendaDTO.setDataVencimento(LocalDate.now());

        List<LinhaVendaDTO> linhasVenda = new ArrayList<>();
        for (LinhaOrcamento linha : orcamento.getLinhas()) {
            LinhaVendaDTO linhaDTO = new LinhaVendaDTO();
            linhaDTO.setArtigoId(linha.getArtigo().getId());
            linhaDTO.setTaxaIvaId(linha.getTaxaIva().getId());
            linhaDTO.setQuantidade(linha.getQuantidade());
            linhaDTO.setPrecoUnitario(linha.getPrecoVendaUnitario());
            linhaDTO.setDesignacaoPersonalizada("Origem: Orçamento #" + orcamento.getId());
            linhasVenda.add(linhaDTO);
        }
        vendaDTO.setLinhas(linhasVenda);
        vendaService.registarVenda(vendaDTO);

        orcamento.setEstado(Orcamento.EstadoOrcamento.CONVERTIDO_VENDA);
        orcamentoRepository.save(orcamento);
        log.debug("Orçamento ID: {} convertido com sucesso.", orcamentoId);
    }

    @Transactional(readOnly = true)
    public Page<OrcamentoResponseDTO> listarMeusOrcamentos(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de orçamentos solicitada pelo utilizador ID: {}. Página: {}", utilizadorId, pagina);

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

    @Transactional
    public void eliminarOrcamento(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de eliminação do Orçamento ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            log.warn("Tentativa de eliminação de orçamento já convertido (ID: {}) bloqueada.", id);
            throw new IllegalArgumentException("Não é possível eliminar um orçamento que já gerou vendas. Arquive-o ou anule a venda primeiro.");
        }
        orcamentoRepository.delete(orcamento);
        log.debug("Orçamento ID: {} eliminado com sucesso.", id);
    }

    @Transactional
    public OrcamentoResponseDTO alterarEstado(Long id, Orcamento.EstadoOrcamento novoEstado) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A alterar o estado do Orçamento ID: {} para {} (Utilizador: {})", id, novoEstado, utilizadorId);

        Orcamento orcamento = orcamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Orçamento não encontrado."));

        if (orcamento.getEstado() == Orcamento.EstadoOrcamento.CONVERTIDO_VENDA) {
            log.warn("Bloqueada alteração de estado no Orçamento ID: {}. Já se encontra convertido em venda.", id);
            throw new IllegalArgumentException("Orçamento já convertido em venda.");
        }
        orcamento.setEstado(novoEstado);

        return converterParaDTO(orcamentoRepository.save(orcamento));
    }

    // 🚀 HELPERS DE PERFORMANCE
    private Map<Long, Artigo> carregarArtigos(List<OrcamentoDTO.LinhaOrcamentoDTO> linhas, Long utilizadorId) {
        List<Long> ids = linhas.stream().map(OrcamentoDTO.LinhaOrcamentoDTO::getArtigoId).distinct().toList();
        return artigoRepository.findAllByIdInAndUtilizadorId(ids, utilizadorId)
                .stream().collect(Collectors.toMap(Artigo::getId, a -> a));
    }

    private Map<Long, TxIva> carregarIvas(List<OrcamentoDTO.LinhaOrcamentoDTO> linhas) {
        List<Long> ids = linhas.stream().map(OrcamentoDTO.LinhaOrcamentoDTO::getTaxaIvaId).distinct().toList();
        return txIvaRepository.findAllById(ids)
                .stream().collect(Collectors.toMap(TxIva::getId, t -> t));
    }

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