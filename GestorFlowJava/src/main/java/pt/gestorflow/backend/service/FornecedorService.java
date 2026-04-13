package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public FornecedorResponseDTO criar (FornecedorDTO dto) {
        Utilizador user = getUtilizadorLogado();
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), user.getId())) {
                throw new RuntimeException("Já existe um fornecedor com este NIF.");
            }
        }

        Fornecedor f = new Fornecedor();
        mapearDtoParaEntidade(dto, f);
        f.setUtilizador(user);
        return converterParaDTO(repository.save(f));
    }

    public FornecedorResponseDTO atualizar(Long id, FornecedorDTO dto) {
        Fornecedor f = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));
        mapearDtoParaEntidade(dto, f);
        return converterParaDTO(repository.save(f));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponseDTO> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    public void eliminar(Long id) {
        Fornecedor f = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado"));

        // Nota: Se tiver compras associadas, o SQL vai lançar exceção (Foreign Key).
        // O ideal seria apanhar essa exceção no Controller ou verificar aqui antes.
        repository.delete(f);
    }

    private FornecedorResponseDTO converterParaDTO(Fornecedor f) {
        FornecedorResponseDTO dto = new FornecedorResponseDTO();
        dto.setId(f.getId());
        dto.setNome(f.getNome());
        dto.setNif(f.getNif());
        dto.setEmail(f.getEmail());
        dto.setTelefone(f.getTelefone());
        dto.setMorada(f.getMorada());
        dto.setWebsite(f.getWebsite());
        return dto;
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