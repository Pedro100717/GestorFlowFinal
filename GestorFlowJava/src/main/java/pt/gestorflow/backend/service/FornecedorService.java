package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository repository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para associar a entidade
    private final AuthService authService; // 🚀 A nossa Chave Mestra de Segurança

    @Transactional
    public FornecedorResponseDTO criar(FornecedorDTO dto) {
        // 🚀 1. Obtém o ID blindado do Token
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Validação de NIF duplicado restrita ao contexto deste utilizador
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                throw new RuntimeException("Já existe um fornecedor com este NIF na sua conta.");
            }
        }

        // 🚀 2. Busca a entidade física do Utilizador
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Fornecedor f = new Fornecedor();
        mapearDtoParaEntidade(dto, f);
        f.setUtilizador(user);

        return converterParaDTO(repository.save(f));
    }

    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Garante que só edita fornecedores da própria conta
        Fornecedor f = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        // 🛡️ Validação de NIF para evitar duplicação em registos diferentes
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(f.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                throw new RuntimeException("Já existe outro fornecedor com este NIF na sua conta.");
            }
        }

        mapearDtoParaEntidade(dto, f);
        return converterParaDTO(repository.save(f));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        return repository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Impede que um utilizador apague dados de terceiros manipulando o ID
        Fornecedor f = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        repository.delete(f);
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ PROTEÇÃO IDOR: Detalhes do fornecedor só visíveis para o dono
        Fornecedor fornecedor = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

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

    private void mapearDtoParaEntidade(FornecedorDTO dto, Fornecedor f) {
        f.setNome(dto.getNome());
        f.setNif(dto.getNif());
        f.setEmail(dto.getEmail());
        f.setTelefone(dto.getTelefone());
        f.setMorada(dto.getMorada());
        f.setWebsite(dto.getWebsite());
    }
}