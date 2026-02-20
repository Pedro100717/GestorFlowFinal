package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.ArtigoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtigoService {

    private final ArtigoRepository artigoRepository;
    private final FamiliaRepository familiaRepository;
    private final TxIvaRepository txIvaRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Artigo criarArtigo(ArtigoDTO dto) {
        Utilizador user = getUtilizadorLogado();
        Artigo artigo;

        // 1. Decidir o Tipo (Mercadoria ou Serviço)
        if (Boolean.TRUE.equals(dto.getMovimentaStock())) {
            Mercadoria m = new Mercadoria();
            m.setStockAtual(BigDecimal.ZERO); // Stock começa a zero
            artigo = m;
        } else {
            artigo = new Servico();
        }

        // 2. Dados Comuns
        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        // Inicializamos preços a zero (serão definidos nas compras/vendas se não vierem do DTO)
        artigo.setPreco(BigDecimal.ZERO);
        artigo.setUltimoPrecoCusto(BigDecimal.ZERO);

        // 3. OBRIGATÓRIO: Definir o Utilizador (AQUI ESTAVA O ERRO)
        artigo.setUtilizador(user);

        // 4. Família (Opcional)
        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada"));
            artigo.setFamilia(familia);
        }

        return artigoRepository.save(artigo);
    }

    public Artigo atualizar(Long id, ArtigoDTO dto) {
        Artigo artigo = artigoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado"));

        artigo.setNome(dto.getNome());
        artigo.setCodigoBarras(dto.getCodigoBarras());

        if (dto.getFamiliaId() != null) {
            Familia familia = familiaRepository.findById(dto.getFamiliaId())
                    .orElseThrow(() -> new EntityNotFoundException("Família não encontrada"));
            artigo.setFamilia(familia);
        } else {
            artigo.setFamilia(null);
        }

        return artigoRepository.save(artigo);
    }

    public Page<Artigo> listarMeusArtigos(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending());
        return artigoRepository.findAllByUtilizadorId(user.getId(), pageable);
    }

    public void eliminar(Long id) {
        if (!artigoRepository.existsById(id)) {
            throw new EntityNotFoundException("Artigo não encontrado");
        }
        artigoRepository.deleteById(id);
    }

    public List<TxIva> listarTaxasIva() {
        return txIvaRepository.findAll();
    }
}