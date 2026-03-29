package pt.gestorflow.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.ClienteDTO;
import pt.gestorflow.backend.dto.ClienteResponseDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ClienteRepository;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // Método auxiliar para saber quem está a fazer o pedido
    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

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

    public ClienteResponseDTO atualizarCliente(Long id, ClienteDTO dto) {
        Cliente cliente = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado ou sem permissão"));

        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());

        return converterParaDTO(repository.save(cliente));
    }

    public Page<ClienteResponseDTO> listarMeusClientes(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());

        //.map() converte automaticamente a pagina de entidades
        return repository.findAllByUtilizadorId(user.getId(), pageable).map(this::converterParaDTO);
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        repository.delete(cliente);
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