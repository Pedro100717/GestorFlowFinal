package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
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

    public SeccaoHomoResponseDTO criar(SeccaoHomoDTO dto) {
        Utilizador user = getUtilizadorLogado();
        CentroCusto pai = centroRepository.findById(dto.getCentroCustoId())
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado"));

        if (!pai.getUtilizador().getId().equals(user.getId())) throw new RuntimeException("Inválido.");

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setCentroCusto(pai);
        sh.setUtilizador(user);

        return converterParaDTO(seccaoRepository.save(sh));
    }

    public List<SeccaoHomoResponseDTO> listar() {
        return seccaoRepository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    public SeccaoHomoResponseDTO atualizar(Long id, SeccaoHomoDTO dto) {
        SeccaoHomo sh = seccaoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Não encontrada"));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        if (!sh.getCentroCusto().getId().equals(dto.getCentroCustoId())) {
            CentroCusto novoPai = centroRepository.findById(dto.getCentroCustoId())
                    .orElseThrow(() -> new EntityNotFoundException("Novo Centro não encontrada"));
            sh.setCentroCusto(novoPai);
        }
        return converterParaDTO(seccaoRepository.save(sh));
    }

    private SeccaoHomoResponseDTO converterParaDTO(SeccaoHomo sh) {
        SeccaoHomoResponseDTO dto = new SeccaoHomoResponseDTO();
        dto.setId(sh.getId());
        dto.setNome(sh.getNome());
        dto.setCodigo(sh.getCodigo());
        if (sh.getCentroCusto() != null) {
            dto.setCentroCustoId(sh.getCentroCusto().getId());
            dto.setCentroCustoNome(sh.getCentroCusto().getNome());
        }
        return dto;
    }
}