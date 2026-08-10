package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Slf4j // 🚀 Lombok toma conta do recado
@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository repository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public CentroCustoResponseDTO criar(CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Registo de Auditoria Financeira
        log.info("Início da criação de um novo Centro de Custo ('{}') para o utilizador ID: {}", dto.getNome(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(user);

        CentroCusto salvo = repository.save(cc);
        log.debug("Centro de Custo '{}' criado com sucesso com o ID: {}", salvo.getNome(), salvo.getId());

        return converterParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de Centros de Custo solicitada pelo utilizador ID: {}", utilizadorId);

        return repository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public CentroCustoResponseDTO atualizar(Long id, CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Centro de Custo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());

        CentroCusto atualizado = repository.save(cc);
        log.debug("Centro de Custo ID: {} atualizado com sucesso", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public CentroCustoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        return converterParaDTO(cc);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Aviso Crítico: Pedido de eliminação do Centro de Custo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        repository.delete(cc);
        log.debug("Centro de Custo ID: {} eliminado com sucesso", id);
    }

    private CentroCustoResponseDTO converterParaDTO(CentroCusto cc) {
        CentroCustoResponseDTO dto = new CentroCustoResponseDTO();
        dto.setId(cc.getId());
        dto.setNome(cc.getNome());
        dto.setCodigo(cc.getCodigo());
        return dto;
    }
}