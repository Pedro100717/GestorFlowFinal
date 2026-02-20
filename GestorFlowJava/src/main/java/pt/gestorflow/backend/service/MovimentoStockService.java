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
    public MovimentoStock registarAcerto(MovimentoStockDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 1. Validar se o Artigo existe e é uma Mercadoria
        Artigo artigo = artigoRepository.findById(dto.getMercadoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        if (!(artigo instanceof Mercadoria mercadoria)) {
            throw new RuntimeException("Apenas mercadorias possuem controlo de stock. Serviços não podem ser ajustados.");
        }

        // Validação de segurança: a mercadoria pertence ao utilizador?
        if (!mercadoria.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Sem permissão para alterar este artigo.");
        }

        // 2. Atualizar o Stock da Mercadoria
        if (dto.getTipo() == MovimentoStock.TipoMovimentoStock.ENTRADA) {
            mercadoria.setStockAtual(mercadoria.getStockAtual().add(dto.getQuantidade()));
        } else {
            // Em quebras/acertos manuais, faz sentido permitir que o stock vá a negativo?
            // Depende da tua regra de negócio. Aqui não bloqueio, mas fica o alerta.
            mercadoria.setStockAtual(mercadoria.getStockAtual().subtract(dto.getQuantidade()));
        }

        artigoRepository.save(mercadoria);

        // 3. Criar o Registo de Auditoria
        MovimentoStock mov = new MovimentoStock();
        mov.setMercadoria(mercadoria);
        mov.setUtilizador(user);
        mov.setTipo(dto.getTipo());
        mov.setQuantidade(dto.getQuantidade());
        mov.setMotivo(dto.getMotivo());
        mov.setDataMovimento(dto.getDataMovimento());
        mov.setStockAposMovimento(mercadoria.getStockAtual()); // Rastreabilidade perfeita

        return movimentoRepository.save(mov);
    }

    public Page<MovimentoStock> listarHistorico(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("dataMovimento").descending());
        return movimentoRepository.findAllByUtilizadorId(getUtilizadorLogado().getId(), pageable);
    }
}