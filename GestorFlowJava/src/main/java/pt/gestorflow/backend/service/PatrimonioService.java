package pt.gestorflow.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.*;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.PatrimonioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatrimonioService {

    private final PatrimonioRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Listar TUDO junto (A tabela vai mostrar tudo misturado, o que é bom para visão geral)
    public List<Patrimonio> listarPatrimonio() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId());
    }

    // --- CRIAR VIATURA ---
    @Transactional
    public PatrimonioViatura criarViatura(PatrimonioViaturaDTO dto) {
        PatrimonioViatura p = new PatrimonioViatura();
        // Dados Base
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());
        // Dados Específicos
        p.setMatricula(dto.getMatricula());
        p.setMarca(dto.getMarca());
        p.setModelo(dto.getModelo());
        p.setValidadeSeguro(dto.getValidadeSeguro());
        p.setProximaInspecao(dto.getProximaInspecao());

        return repository.save(p);
    }

    // --- CRIAR IMÓVEL ---
    @Transactional
    public PatrimonioImovel criarImovel(PatrimonioImovelDTO dto) {
        PatrimonioImovel p = new PatrimonioImovel();
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());

        p.setMorada(dto.getMorada());
        p.setArtigoMatricial(dto.getArtigoMatricial());
        p.setTipo(dto.getTipo());

        return repository.save(p);
    }

    // --- CRIAR FERRAMENTA ---
    @Transactional
    public PatrimonioFerramenta criarFerramenta(PatrimonioFerramentaDTO dto) {
        PatrimonioFerramenta p = new PatrimonioFerramenta();
        p.setNome(dto.getNome());
        p.setDataAquisicao(dto.getDataAquisicao());
        p.setValorAquisicao(dto.getValorAquisicao());
        p.setUtilizador(getUtilizadorLogado());

        p.setNumeroSerie(dto.getNumeroSerie());
        p.setEstadoConservacao(dto.getEstadoConservacao());

        return repository.save(p);
    }

    // Apagar qualquer coisa
    public void eliminar(Long id) {
        // Validação de segurança básica
        Patrimonio p = repository.findById(id).orElseThrow();
        if(!p.getUtilizador().getId().equals(getUtilizadorLogado().getId())) throw new RuntimeException("Proibido");
        repository.deleteById(id);
    }
}