package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Adicionado
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.dto.ArtigoResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;

@Slf4j // 🚀 Lombok Logger ativado
@Service
@RequiredArgsConstructor
public class ArtigoService {

    private final ArtigoRepository artigoRepository;
    private final FamiliaRepository familiaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public ArtigoResponseDTO criarArtigo(ArtigoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Início da criação de artigo para o utilizador ID: {}", utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Artigo artigo;

        if (Boolean.TRUE.equals(dto.getMovimentaStock())) {
            Mercadoria m = new Mercadoria();
            m.setStockAtual(BigDecimal.ZERO);
            artigo = m;
        } else {
            artigo = new Servico();
        }

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());
        artigo.setPreco(BigDecimal.ZERO);
        artigo.setUltimoPrecoCusto(BigDecimal.ZERO);
        artigo.setUtilizador(user);

        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        }

        Artigo salvo = artigoRepository.save(artigo);
        log.debug("Artigo criado com sucesso com ID: {} pelo utilizador ID: {}", salvo.getId(), utilizadorId);

        return converterParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ArtigoResponseDTO> listarMeusArtigos(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // Em listagens não vale a pena fazer INFO. Fica apenas como DEBUG se precisarmos de auditar pesquisas.
        log.debug("Listagem de artigos solicitada pelo utilizador ID: {}. Página: {}", utilizadorId, pagina);

        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        return artigoRepository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional
    public ArtigoResponseDTO atualizar(Long id, ArtigoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Pedido de atualização do artigo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findByIdAndUtilizadorId(dto.getFamiliaId(), utilizadorId)
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada ou acesso negado."));
            artigo.setFamilia(familia);
        } else {
            artigo.setFamilia(null);
        }

        Artigo atualizado = artigoRepository.save(artigo);
        log.debug("Artigo ID: {} atualizado com sucesso", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Pedido de eliminação do artigo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        artigoRepository.delete(artigo);
        log.debug("Artigo ID: {} eliminado com sucesso da base de dados", id);
    }

    @Transactional(readOnly = true)
    public ArtigoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        return converterParaDTO(artigo);
    }

    @Transactional
    public void adicionarStock(Long artigoId, BigDecimal quantidade) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Pedido para adicionar {} de stock ao artigo ID: {} (Utilizador: {})", quantidade, artigoId, utilizadorId);

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(artigoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (artigo instanceof Mercadoria m) {
            m.setStockAtual(m.getStockAtual().add(quantidade));
            artigoRepository.save(m);
            log.debug("Stock adicionado. Novo stock: {}", m.getStockAtual());
        } else {
            // 🛡️ CORREÇÃO DE LÓGICA: Lança erro em vez de ignorar!
            throw new IllegalArgumentException("Não é possível adicionar stock a um Serviço.");
        }
    }

    @Transactional
    public void removerStock(Long artigoId, BigDecimal quantidade) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        log.info("Pedido para remover {} de stock ao artigo ID: {} (Utilizador: {})", quantidade, artigoId, utilizadorId);

        Artigo artigo = artigoRepository.findByIdAndUtilizadorId(artigoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou acesso negado."));

        if (artigo instanceof Mercadoria m) {
            m.setStockAtual(m.getStockAtual().subtract(quantidade));
            artigoRepository.save(m);
            log.debug("Stock removido. Novo stock: {}", m.getStockAtual());
        } else {
            // 🛡️ CORREÇÃO DE LÓGICA: Lança erro em vez de ignorar!
            throw new IllegalArgumentException("Não é possível remover stock de um Serviço.");
        }
    }

    // --- CONVERSOR INTERNO ---
    private ArtigoResponseDTO converterParaDTO(Artigo a) {
        ArtigoResponseDTO dto = new ArtigoResponseDTO();
        dto.setId(a.getId());
        dto.setNome(a.getNome());
        dto.setCodigoBarras(a.getCodigoBarras());
        dto.setPreco(a.getPreco());
        dto.setUltimoPrecoCusto(a.getUltimoPrecoCusto());
        dto.setMovimentaStock(a.isMovimentaStock());

        if (a instanceof Mercadoria m) {
            dto.setTipo("MERCADORIA");
            dto.setStockAtual(m.getStockAtual());
        } else {
            dto.setTipo("SERVICO");
            dto.setStockAtual(null);
        }

        if (a.getFamilia() != null) {
            dto.setFamiliaId(a.getFamilia().getId());
            dto.setFamiliaNome(a.getFamilia().getNome());
        }

        return dto;
    }
}