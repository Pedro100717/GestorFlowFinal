package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;
import pt.gestorflow.backend.repository.SeccaoHomoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeccaoHomoService {

    private final SeccaoHomoRepository seccaoRepository;
    private final CentroCustoRepository centroRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public List<SeccaoHomo> listar() {
        return seccaoRepository.findAllByUtilizadorId(getUtilizadorLogado().getId());
    }

    public SeccaoHomo criar(SeccaoHomoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // LÓGICA: Validar se o Centro de Custo existe e pertence a este utilizador
        CentroCusto pai = centroRepository.findById(dto.getCentroCustoId())
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado"));

        if (!pai.getUtilizador().getId().equals(user.getId())) {
            throw new RuntimeException("Centro de Custo inválido.");
        }

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setCentroCusto(pai); // <--- LIGAÇÃO FEITA
        sh.setUtilizador(user);

        return seccaoRepository.save(sh);
    }

    public SeccaoHomo atualizar(Long id, SeccaoHomoDTO dto) {
        SeccaoHomo sh = seccaoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Secção não encontrada"));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        // Se o utilizador mudou o Centro de Custo no dropdown
        if (!sh.getCentroCusto().getId().equals(dto.getCentroCustoId())) {
            CentroCusto novoPai = centroRepository.findById(dto.getCentroCustoId())
                    .orElseThrow(() -> new EntityNotFoundException("Novo Centro de Custo não encontrado"));
            sh.setCentroCusto(novoPai);
        }

        return seccaoRepository.save(sh);
    }

    public void eliminar(Long id) {
        if (!seccaoRepository.existsById(id)) throw new EntityNotFoundException("Não encontrado");
        seccaoRepository.deleteById(id);
    }
}