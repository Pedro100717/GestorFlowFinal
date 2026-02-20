package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.CentroCustoDTO;
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

    public List<CentroCusto> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId());
    }

    public CentroCusto criar(CentroCustoDTO dto) {
        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(getUtilizadorLogado());
        return repository.save(cc);
    }

    public CentroCusto atualizar(Long id, CentroCustoDTO dto) {
        CentroCusto cc = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado"));

        // Validação extra: O CC pertence ao user logado? (O Spring Data JPA já filtra no findById se configurado, mas é bom garantir)
        if (!cc.getUtilizador().getId().equals(getUtilizadorLogado().getId())) {
            throw new EntityNotFoundException("Não tens permissão para editar este registo.");
        }

        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        return repository.save(cc);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) throw new EntityNotFoundException("Não encontrado");
        repository.deleteById(id);
    }
}