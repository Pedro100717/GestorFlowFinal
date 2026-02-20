package pt.gestorflow.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;

@Service
public class UtilizadorService {

    private final UtilizadorRepository repository;

    // 1. AQUI ESTAVA O ERRO: Tens de declarar a variável aqui
    private final PasswordEncoder passwordEncoder;

    // 2. E tens de a receber no Construtor (Injeção de Dependência)
    public UtilizadorService(UtilizadorRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Utilizador registarNovoUtilizador(RegistoDTO dados) {
        // Validações
        if (repository.existsByEmail(dados.getEmail())) {
            throw new RuntimeException("Erro: Este email já está registado.");
        }
        if (repository.existsByNomeUtilizador(dados.getNomeUtilizador())) {
            throw new RuntimeException("Erro: Este nome de utilizador já existe.");
        }

        // Criar Entidade
        Utilizador novoUser = new Utilizador();
        novoUser.setNomeUtilizador(dados.getNomeUtilizador());
        novoUser.setEmail(dados.getEmail());

        String senhaHash = passwordEncoder.encode(dados.getSenha());
        novoUser.setSenha(senhaHash);

        return repository.save(novoUser);
    }

    public List<Utilizador> listarTodos() {
        return repository.findAll();
    }
}