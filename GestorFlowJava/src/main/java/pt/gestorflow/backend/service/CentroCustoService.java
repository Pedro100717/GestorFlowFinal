package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public CentroCustoResponseDTO criar(CentroCustoDTO dto) {
        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(getUtilizadorLogado());
        return converterParaDTO(repository.save(cc));
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    public CentroCustoResponseDTO atualizar(Long id, CentroCustoDTO dto) {
        CentroCusto cc = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Não encontrado"));
        if (!cc.getUtilizador().getId().equals(getUtilizadorLogado().getId())) {
            throw new EntityNotFoundException("Acesso negado");
        }
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        return converterParaDTO(repository.save(cc));
    }

    private CentroCustoResponseDTO converterParaDTO(CentroCusto cc) {
        CentroCustoResponseDTO dto = new CentroCustoResponseDTO();
        dto.setId(cc.getId());
        dto.setNome(cc.getNome());
        dto.setCodigo(cc.getCodigo());
        return dto;
    }
}