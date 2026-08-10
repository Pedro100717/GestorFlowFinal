package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.SeccaoHomoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class SeccaoHomoService {

    private final SeccaoHomoRepository seccaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public SeccaoHomoResponseDTO criar(SeccaoHomoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de nova Secção Homogénea ('{}') para o utilizador ID: {}", dto.getNome(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setUtilizador(user);

        SeccaoHomo salva = seccaoRepository.save(sh); // 🚀 Variável chama-se "salva"
        log.debug("Secção Homogénea criada com sucesso com o ID: {}", salva.getId());

        return converterParaDTO(salva); // 🚀 Corrigido aqui para "salva" também!
    }

    @Transactional(readOnly = true)
    public List<SeccaoHomoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de Secções Homogéneas solicitada pelo utilizador ID: {}", utilizadorId);

        return seccaoRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public SeccaoHomoResponseDTO atualizar(Long id, SeccaoHomoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização da Secção Homogénea ID: {} pelo utilizador ID: {}", id, utilizadorId);

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção não encontrada ou acesso negado."));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        SeccaoHomo atualizada = seccaoRepository.save(sh);
        log.debug("Secção Homogénea ID: {} atualizada com sucesso.", atualizada.getId());

        return converterParaDTO(atualizada);
    }

    @Transactional(readOnly = true)
    public SeccaoHomoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

        return converterParaDTO(sh);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: Pedido de eliminação da Secção Homogénea ID: {} pelo utilizador ID: {}", id, utilizadorId);

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

        seccaoRepository.delete(sh);
        log.debug("Secção Homogénea ID: {} eliminada com sucesso.", id);
    }

    private SeccaoHomoResponseDTO converterParaDTO(SeccaoHomo sh) {
        SeccaoHomoResponseDTO dto = new SeccaoHomoResponseDTO();
        dto.setId(sh.getId());
        dto.setNome(sh.getNome());
        dto.setCodigo(sh.getCodigo());
        return dto;
    }
}