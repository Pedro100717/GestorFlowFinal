package pt.gestorflow.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.ClienteDTO;
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

    public Cliente criarCliente(ClienteDTO dto) {
        Utilizador user = getUtilizadorLogado();

        // Validar NIF duplicado (dentro da conta deste user)
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

        // Associa ao utilizador logado
        cliente.setUtilizador(user);

        return repository.save(cliente);
    }

    public Cliente atualizarCliente(Long id, ClienteDTO dto) {
        Cliente cliente = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado ou sem permissão"));

        // Atualizar campos
        cliente.setNome(dto.getNome());
        cliente.setNif(dto.getNif());
        cliente.setEmail(dto.getEmail());
        cliente.setTelefone(dto.getTelefone());
        cliente.setMorada(dto.getMorada());
        cliente.setAnotacoes(dto.getAnotacoes());

        return repository.save(cliente);
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = repository.findByIdAndUtilizadorId(id, getUtilizadorLogado().getId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        repository.delete(cliente);
    }

    public Page<Cliente> listarMeusClientes(int pagina, int tamanho) {
        Utilizador user = getUtilizadorLogado();

        // Cria a instrução: "Quero a página X, com Y itens, ordenado por ID decrescente (mais recentes primeiro)"
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());

        return repository.findAllByUtilizadorId(user.getId(), pageable);
    }
}