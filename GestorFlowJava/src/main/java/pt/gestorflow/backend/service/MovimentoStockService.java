package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.MovimentoStockDTO;
import pt.gestorflow.backend.dto.MovimentoStockResponseDTO; // NOVO IMPORT
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.ArtigoRepository;
import pt.gestorflow.backend.repository.MovimentoStockRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MovimentoStockService {

    private final MovimentoStockRepository movimentoRepository;
    private final ArtigoRepository artigoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public MovimentoStockResponseDTO registarAcerto(MovimentoStockDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Artigo artigo = artigoRepository.findById(dto.getMercadoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        if (!(artigo instanceof Mercadoria mercadoria)) {
            throw new RuntimeException("Apenas mercadorias possuem controlo de stock.");
        }

        if (!mercadoria.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Sem permissão para alterar este artigo.");
        }

        if (dto.getTipo() == MovimentoStock.TipoMovimentoStock.ENTRADA) {
            mercadoria.setStockAtual(mercadoria.getStockAtual().add(dto.getQuantidade()));
        } else {
            mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(dto.getQuantidade()));
        }

        artigoRepository.save(mercadoria);

        MovimentoStock mov = new MovimentoStock();
        mov.setMercadoria(mercadoria);
        mov.setUtilizador(user);
        mov.setTipo(dto.getTipo());
        mov.setQuantidade(dto.getQuantidade());
        mov.setMotivo(dto.getMotivo());
        mov.setDataMovimento(dto.getDataMovimento());
        mov.setStockAposMovimento(mercadoria.getStockAtual());

        MovimentoStock movGuardado = movimentoRepository.save(mov);

        // Devolve o envelope DTO limpo!
        return converterParaDTO(movGuardado);
    }

    public Page<MovimentoStockResponseDTO> listarHistorico(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataMovimento").descending());
        // A magia de converter toda a lista
        return movimentoRepository.findAllByUtilizadorId(getUtilizadorLogado().getId(), pageable).map(this::converterParaDTO);
    }

    // ==========================================
    // CONVERSOR INTERNO
    // ==========================================
    private MovimentoStockResponseDTO converterParaDTO(MovimentoStock m) {
        MovimentoStockResponseDTO dto = new MovimentoStockResponseDTO();
        dto.setId(m.getId());
        dto.setDataMovimento(m.getDataMovimento());
        dto.setTipo(m.getTipo().name());
        dto.setQuantidade(m.getQuantidade());
        dto.setMotivo(m.getMotivo());
        dto.setStockAposMovimento(m.getStockAposMovimento());

        if (m.getMercadoria() != null) {
            dto.setMercadoriaId(m.getMercadoria().getId());
            dto.setMercadoriaNome(m.getMercadoria().getNome());
        }
        return dto;
    }
}