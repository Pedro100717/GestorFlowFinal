package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.FornecedorDTO;
import pt.gestorflow.backend.dto.FornecedorResponseDTO;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public Page<FornecedorResponseDTO> listar(Pageable pageable) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem paginada de fornecedores solicitada pelo utilizador ID: {}", utilizadorId);

        // 🚀 O Page do Spring já faz o map diretamente para o teu DTO
        return repository.findAllByUtilizadorId(utilizadorId, pageable)
                .map(this::converterParaDTO);
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

    @Transactional
    public List<FornecedorResponseDTO> importarEmLote(List<FornecedorDTO> dtos) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar importação em lote de {} fornecedores para o utilizador ID: {}", dtos.size(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        List<Fornecedor> fornecedoresParaGuardar = new ArrayList<>();
        Set<String> nifsNoLote = new HashSet<>();

        for (int i = 0; i < dtos.size(); i++) {
            FornecedorDTO dto = dtos.get(i);
            int linhaReal = i + 1; // Para darmos um erro humano (Linha 1, Linha 2...)

            // 🛡️ Validação 1: Nome é estritamente obrigatório
            if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O Nome do fornecedor é obrigatório.");
            }

            // 🛡️ Validação 2: NIF é obrigatório
            if (dto.getNif() == null || dto.getNif().trim().isEmpty()) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O NIF é obrigatório.");
            }

            String nifLimpo = dto.getNif().trim();

            // 🛡️ Validação 3: NIF duplicado dentro do próprio Excel
            if (!nifsNoLote.add(nifLimpo)) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O NIF " + nifLimpo + " está repetido no ficheiro.");
            }

            // 🛡️ Validação 4: NIF já existe na Base de Dados?
            if (repository.existsByNifAndUtilizadorId(nifLimpo, utilizadorId)) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": Já existe um fornecedor com o NIF " + nifLimpo + " na sua conta.");
            }

            // Mapeamento (Os campos opcionais podem ir a null sem problema)
            Fornecedor fornecedor = new Fornecedor();
            fornecedor.setNome(dto.getNome().trim());
            fornecedor.setNif(nifLimpo);
            fornecedor.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
            fornecedor.setTelefone(dto.getTelefone() != null ? dto.getTelefone().trim() : null);
            fornecedor.setMorada(dto.getMorada() != null ? dto.getMorada().trim() : null);
            fornecedor.setWebsite(dto.getWebsite() != null ? dto.getWebsite().trim() : null); // O campo específico do fornecedor
            fornecedor.setUtilizador(user);

            fornecedoresParaGuardar.add(fornecedor);
        }

        List<Fornecedor> guardados = repository.saveAll(fornecedoresParaGuardar);
        log.debug("Importação concluída. {} fornecedores guardados na Base de Dados.", guardados.size());

        return guardados.stream().map(this::converterParaDTO).toList();
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