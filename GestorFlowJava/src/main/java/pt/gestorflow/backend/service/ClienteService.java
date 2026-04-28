package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final UtilizadorRepository utilizadorRepository; // 🚀 Necessário para obter a entidade Utilizador
    private final AuthService authService; // 🚀 A nossa nova âncora de segurança

    @Transactional
    public ClienteResponseDTO criarCliente(ClienteDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Validação de NIF duplicado restrita ao contexto do utilizador logado
        if (dto.getNif() != null && !dto.getNif().isBlank()) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
                throw new RuntimeException("Já existe um cliente com este NIF na sua conta.");
            }
        }

        // 🛡️ Obtém a entidade Utilizador garantida pelo Token
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

        return converterParaDTO(repository.save(cliente));
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(Long id, ClienteDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Localiza o cliente apenas se pertencer ao utilizador logado
        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        // 🛡️ Validação de NIF no Update para evitar duplicação em registos existentes
        if (dto.getNif() != null && !dto.getNif().isBlank() && !dto.getNif().equals(cliente.getNif())) {
            if (repository.existsByNifAndUtilizadorId(dto.getNif(), utilizadorId)) {
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
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id").descending());

        return repository.findAllByUtilizadorId(utilizadorId, pageable).map(this::converterParaDTO);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Garante que um utilizador malicioso não apaga clientes de terceiros
        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        repository.delete(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Proteção IDOR: Garante que os detalhes do cliente só são visíveis para o dono
        Cliente cliente = repository.findByIdAndUtilizadorId(id, utilizadorId)
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