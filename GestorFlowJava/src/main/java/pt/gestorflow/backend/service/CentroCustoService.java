package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository repository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para associar a entidade no 'criar'
    private final AuthService authService; // 🚀 A nossa nova fonte de verdade para segurança

    @Transactional
    public CentroCustoResponseDTO criar(CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Garante que o utilizador existe antes de tentar associar
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(user);

        return converterParaDTO(repository.save(cc));
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        return repository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public CentroCustoResponseDTO atualizar(Long id, CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Só permite atualizar se o ID pertencer ao utilizador logado
        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());

        return converterParaDTO(repository.save(cc));
    }

    @Transactional(readOnly = true)
    public CentroCustoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Garante que um utilizador não lê dados de outro através do ID
        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        return converterParaDTO(cc);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Bloqueia tentativas de eliminação maliciosas via ID
        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        repository.delete(cc);
    }

    private CentroCustoResponseDTO converterParaDTO(CentroCusto cc) {
        CentroCustoResponseDTO dto = new CentroCustoResponseDTO();
        dto.setId(cc.getId());
        dto.setNome(cc.getNome());
        dto.setCodigo(cc.getCodigo());
        return dto;
    }
}