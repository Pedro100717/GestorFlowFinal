package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- CRÍTICO
import pt.gestorflow.backend.dto.ClienteDTO;
import pt.gestorflow.backend.dto.ClienteResponseDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;

@Service
@RequiredArgsConstructor // Padronizado com o resto do projeto (remove o construtor manual)
public class ClienteService {

    private final ClienteRepository repository;

    // Método auxiliar para saber quem está a fazer o pedido
    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public ClienteResponseDTO criarCliente(ClienteDTO dto) {
        Utilizador user = getUtilizadorLogado();

        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), user.getId())) {
                throw new RuntimeException("Já existe um cliente com este NIF na sua conta.");
            }
        }

        Cliente cliente = new Cliente();
        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());
        cliente.setUtilizador(user);

        return converterParaDTO(repository.save(cliente));
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(Long id, ClienteDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ CORREÇÃO: Usar a exceção correta para dar HTTP 404
        Cliente cliente = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou sem permissão"));

        // 🛡️ CORREÇÃO: Impedir duplicação de NIF no Update!
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(cliente.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), user.getId())) {
                throw new RuntimeException("Já existe outro cliente com este NIF na sua conta.");
            }
        }

        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());

        return converterParaDTO(repository.save(cliente));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listarMeusClientes(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());

        return repository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        // 🛡️ CORREÇÃO: EntityNotFoundException
        Cliente cliente = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        // Nota Industrial: Se o cliente tiver Vendas associadas, o delete() vai estoirar
        // com um erro de Foreign Key (DataIntegrityViolationException).
        // O teu GlobalExceptionHandler apanha o erro genérico, mas deves testar isto no frontend.
        repository.delete(cliente);
    }

    // 🛡️ ADICIONADO: Método para buscar os detalhes de um único cliente
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR CRÍTICA: Impedir que um utilizador veja clientes de outra conta
        Cliente cliente = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        return converterParaDTO(cliente);
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