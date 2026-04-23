package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🛡️ CRÍTICO
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

    // 🛡️ ADICIONADO: Proteção de transação
    @Transactional
    public FornecedorResponseDTO criar(FornecedorDTO dto) {
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

    // 🛡️ ADICIONADO: Proteção de transação
    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorDTO dto) {
        Utilizador user = getUtilizadorLogado();

        Fornecedor f = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        // 🛡️ CORREÇÃO CRÍTICA: Impedir duplicação de NIF no Update!
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(f.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), user.getId())) {
                throw new RuntimeException("Já existe outro fornecedor com este NIF na sua conta.");
            }
        }

        mapearDtoParaEntidade(dto, f);
        return converterParaDTO(repository.save(f));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponseDTO> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    // 🛡️ ADICIONADO: Proteção de transação
    @Transactional
    public void eliminar(Long id) {
        Fornecedor f = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        // Nota Industrial: Como bem escreveste no teu comentário original, se houver compras,
        // o Hibernate atira um DataIntegrityViolationException. O teu GlobalExceptionHandler
        // vai apanhar isso pelo Exception genérico e atirar 500. Idealmente, no futuro,
        // capturas o DataIntegrityViolationException lá e atiras um 400 amigável para o Angular.
        repository.delete(f);
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR: Garante que não tentam ler fornecedores de outra empresa
        Fornecedor fornecedor = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        // Assumo que já tens um método converterParaDTO no teu FornecedorService!
        return converterParaDTO(fornecedor);
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

    // Método auxiliar mantido, mas agora seguro porque a validação é feita antes de o chamar
    private void mapearDtoParaEntidade(FornecedorDTO dto, Fornecedor f) {
        f.setNome(dto.getNome());
        f.setNif(dto.getNif());
        f.setEmail(dto.getEmail());
        f.setTelefone(dto.getTelefone());
        f.setMorada(dto.getMorada());
        f.setWebsite(dto.getWebsite());
    }
}