package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository repository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public FornecedorResponseDTO criar(FornecedorDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de novo fornecedor para o utilizador ID: {}", utilizadorId);

        // 🛡️ Validação de NIF duplicado restrita ao contexto deste utilizador
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                // 🚀 Corrigido para IllegalArgumentException e log de aviso
                log.warn("Bloqueada tentativa de criar fornecedor com NIF duplicado ({}) para o utilizador ID: {}", dto.getNif(), utilizadorId);
                throw new IllegalArgumentException("Já existe um fornecedor com este NIF na sua conta.");
            }
        }

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Fornecedor f = new Fornecedor();
        mapearDtoParaEntidade(dto, f);
        f.setUtilizador(user);

        Fornecedor salvo = repository.save(f);
        log.debug("Fornecedor criado com sucesso com o ID: {}", salvo.getId());

        return converterParaDTO(salvo);
    }

    @Transactional
    public FornecedorResponseDTO atualizar(Long id, FornecedorDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Fornecedor ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Fornecedor f = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        // 🛡️ Validação de NIF para evitar duplicação em registos diferentes
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(f.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                // 🚀 Corrigido para IllegalArgumentException e log de aviso
                log.warn("Bloqueada tentativa de atualizar fornecedor ID: {} com NIF duplicado ({}) para o utilizador ID: {}", id, dto.getNif(), utilizadorId);
                throw new IllegalArgumentException("Já existe outro fornecedor com este NIF na sua conta.");
            }
        }

        mapearDtoParaEntidade(dto, f);

        Fornecedor atualizado = repository.save(f);
        log.debug("Fornecedor ID: {} atualizado com sucesso", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de fornecedores solicitada pelo utilizador ID: {}", utilizadorId);

        return repository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de eliminação do Fornecedor ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Fornecedor f = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        repository.delete(f);
        log.debug("Fornecedor ID: {} eliminado com sucesso", id);
    }

    @Transactional(readOnly = true)
    public FornecedorResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

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