package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

@Service
@RequiredArgsConstructor
public class UtilizadorService {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;

    // --- 1. REGISTO ---
    @Transactional
    public PerfilResponseDTO registarNovoUtilizador(RegistoDTO dados) {
        if (repository.existsByEmail(dados.getEmail()) || repository.existsByNomeUtilizador(dados.getNomeUtilizador())) {
            // Em produção, uma mensagem genérica previne ataques de "enumeração de contas"
            throw new IllegalArgumentException("Os dados introduzidos são inválidos ou já estão em uso.");
        }

        Utilizador novoUser = new Utilizador();
        novoUser.setNomeUtilizador(dados.getNomeUtilizador());
        novoUser.setEmail(dados.getEmail());
        novoUser.setSenha(passwordEncoder.encode(dados.getSenha()));

        Utilizador guardado = repository.save(novoUser);

        return converterParaDTO(guardado);
    }

    // --- 2. PERFIL ---
    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO obterPerfil() {
        Utilizador userAutenticado = getUtilizadorLogado();

        Utilizador user = repository.findById(userAutenticado.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado"));

        return converterParaDTO(user);
    }

    // --- 3. CONVERSOR ---
    private PerfilResponseDTO converterParaDTO(Utilizador user) {
        PerfilResponseDTO dto = new PerfilResponseDTO();
        dto.setId(user.getId());
        dto.setNomeUtilizador(user.getNomeUtilizador());
        dto.setEmail(user.getEmail());
        return dto;
    }
}