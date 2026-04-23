package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🛡️ CRÍTICO
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.SeccaoHomoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeccaoHomoService {

    private final SeccaoHomoRepository seccaoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional // 🛡️ Adicionado para garantir atomicidade
    public SeccaoHomoResponseDTO criar(SeccaoHomoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setUtilizador(user);

        return converterParaDTO(seccaoRepository.save(sh));
    }

    @Transactional(readOnly = true)
    public List<SeccaoHomoResponseDTO> listar() {
        return seccaoRepository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    @Transactional // 🛡️ Adicionado
    public SeccaoHomoResponseDTO atualizar(Long id, SeccaoHomoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ CORREÇÃO IDOR: Impede editar secções de terceiros
        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Secção não encontrada ou acesso negado."));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        return converterParaDTO(seccaoRepository.save(sh));
    }

    @Transactional(readOnly = true)
    public SeccaoHomoResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogenea não encontrado ou acesso negado"));

        return converterParaDTO(sh);
    }

    @Transactional
    public void eliminar(Long id) {
        Utilizador user = getUtilizadorLogado();

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogenea não encontrada ou acesso negado"));

        seccaoRepository.delete(sh);
    }

    private SeccaoHomoResponseDTO converterParaDTO(SeccaoHomo sh) {
        SeccaoHomoResponseDTO dto = new SeccaoHomoResponseDTO();
        dto.setId(sh.getId());
        dto.setNome(sh.getNome());
        dto.setCodigo(sh.getCodigo());
        return dto;
    }
}