package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.SeccaoHomoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeccaoHomoService {

    private final SeccaoHomoRepository seccaoRepository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para associar na criação
    private final AuthService authService; // 🚀 A nossa Chave Mestra

    @Transactional
    public SeccaoHomoResponseDTO criar(SeccaoHomoDTO dto) {
        // 🚀 1. ID Blindado do Token
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 2. Busca a entidade física do Utilizador
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setUtilizador(user);

        return converterParaDTO(seccaoRepository.save(sh));
    }

    @Transactional(readOnly = true)
    public List<SeccaoHomoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        return seccaoRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public SeccaoHomoResponseDTO atualizar(Long id, SeccaoHomoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Impede editar secções de terceiros
        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção não encontrada ou acesso negado."));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        return converterParaDTO(seccaoRepository.save(sh));
    }

    @Transactional(readOnly = true)
    public SeccaoHomoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Garante a privacidade dos dados baseada no dono
        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

        return converterParaDTO(sh);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Impede eliminações maliciosas
        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

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