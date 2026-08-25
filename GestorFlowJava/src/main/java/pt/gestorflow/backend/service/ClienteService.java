package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.ClienteDTO;
import pt.gestorflow.backend.dto.ClienteResponseDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public ClienteResponseDTO criarCliente(ClienteDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de novo cliente para o utilizador ID: {}", utilizadorId);

        // 🛡️ Validação de NIF duplicado restrita ao contexto do utilizador logado
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                // 🚀 Adicionado log de aviso e corrigida a Exceção para o Handler correto!
                log.warn("Bloqueada tentativa de criar cliente com NIF duplicado ({}) para o utilizador ID: {}", dto.getNif(), utilizadorId);
                throw new IllegalArgumentException("Já existe um cliente com este NIF na sua conta.");
            }
        }

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        Cliente cliente = new Cliente();
        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());
        cliente.setUtilizador(user);

        Cliente salvo = repository.save(cliente);
        log.debug("Cliente criado com sucesso com o ID: {}", salvo.getId());

        return converterParaDTO(salvo);
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(Long id, ClienteDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Cliente ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        // 🛡️ Validação de NIF no Update para evitar duplicação em registos existentes
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(cliente.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                // 🚀 Adicionado log de aviso e corrigida a Exceção
                log.warn("Bloqueada tentativa de atualizar cliente ID: {} com NIF duplicado ({}) para o utilizador ID: {}", id, dto.getNif(), utilizadorId);
                throw new IllegalArgumentException("Já existe outro cliente com este NIF na sua conta.");
            }
        }

        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());

        Cliente atualizado = repository.save(cliente);
        log.debug("Cliente ID: {} atualizado com sucesso", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listarMeusClientes(int pagina, int tamanho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de clientes solicitada pelo utilizador ID: {}. Página: {}", utilizadorId, pagina);

        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());
        return repository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de eliminação do Cliente ID: {} pelo utilizador ID: {}", id, utilizadorId);

        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        repository.delete(cliente);
        log.debug("Cliente ID: {} eliminado com sucesso", id);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        return converterParaDTO(cliente);
    }

    @Transactional
    public List<ClienteResponseDTO> importarEmLote(List<ClienteDTO> dtos) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar importacao em lote {} clientes para o utilizador ID: {}", dtos.size(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador nao encontrado."));

        List<Cliente> clientesParaGuardar = new ArrayList<>();
        Set<String> nifsNoLote = new HashSet<>();

        for (int i = 0; i < dtos.size(); i++) {
            ClienteDTO dto = dtos.get(i);
            int linhaReal = i + 1;

            //Nome obrigatorio
            if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O Nome do cliente é obrigatório.");
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
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": Já existe um cliente com o NIF " + nifLimpo + " na sua conta.");
            }

            Cliente cliente = new Cliente();
            cliente.setNome(dto.getNome().trim());
            cliente.setNif(nifLimpo);
            cliente.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
            cliente.setTelefone(dto.getTelefone() != null ? dto.getTelefone().trim() : null);
            cliente.setMorada(dto.getMorada() != null ? dto.getMorada().trim() : null);
            cliente.setAnotacoes(dto.getAnotacoes() != null ? dto.getAnotacoes().trim() : null);
            cliente.setUtilizador(user);

            clientesParaGuardar.add(cliente);
        }

        List<Cliente> guardados = repository.saveAll(clientesParaGuardar);
        log.debug("Importacao concluida. {} clientes guardados na Base de Dados.", guardados.size());

        return guardados.stream().map(this::converterParaDTO).toList();
    }

    private ClienteResponseDTO converterParaDTO(Cliente c) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setNif(c.getNif());
        dto.setEmail(c.getEmail());
        dto.setTelefone(c.getTelefone());
        dto.setMorada(c.getMorada());
        dto.setAnotacoes(c.getAnotacoes());
        return dto;
    }
}