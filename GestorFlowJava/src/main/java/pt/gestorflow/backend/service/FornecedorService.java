package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.FornecedorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public List<Fornecedor> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId());
    }

    public Fornecedor criar(FornecedorDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // Validação de NIF duplicado
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), user.getId())) {
                throw new RuntimeException("Já existe um fornecedor com este NIF.");
            }
        }

        Fornecedor f = new Fornecedor();
        mapearDtoParaEntidade(dto, f);
        f.setUtilizador(user);

        return repository.save(f);
    }

    public Fornecedor atualizar(Long id, FornecedorDTO dto) {
        Fornecedor f = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        mapearDtoParaEntidade(dto, f);
        return repository.save(f);
    }

    public void eliminar(Long id) {
        Fornecedor f = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        // Nota: Se tiver compras associadas, o SQL vai lançar exceção (Foreign Key).
        // O ideal seria apanhar essa exceção no Controller ou verificar aqui antes.
        repository.delete(f);
    }

    // Método auxiliar para não repetir código
    private void mapearDtoParaEntidade(FornecedorDTO dto, Fornecedor f) {
        f.setNome(dto.getNome());
        f.setNif(dto.getNif());
        f.setEmail(dto.getEmail());
        f.setTelefone(dto.getTelefone());
        f.setMorada(dto.getMorada());
        f.setWebsite(dto.getWebsite());
    }
}