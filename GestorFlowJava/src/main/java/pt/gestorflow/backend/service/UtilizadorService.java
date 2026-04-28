package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    private final AuthService authService; // 🚀 A nossa Chave Mestra

    // --- 1. REGISTO ---
    @Transactional
    public PerfilResponseDTO registarNovoUtilizador(RegistoDTO dados) {
        // Validação "barata" primeiro (evita gastos desnecessários de CPU com hashing)
        if (repository.existsByEmail(dados.getEmail()) || repository.existsByNomeUtilizador(dados.getNomeUtilizador())) {
            throw new IllegalArgumentException("Os dados introduzidos são inválidos ou já estão em uso.");
        }

        Utilizador novoUser = new Utilizador();
        novoUser.setNomeUtilizador(dados.getNomeUtilizador());
        novoUser.setEmail(dados.getEmail());
        // Operação "cara" de CPU só depois das validações
        novoUser.setSenha(passwordEncoder.encode(dados.getSenha()));

        Utilizador guardado = repository.save(novoUser);

        return converterParaDTO(guardado);
    }

    // --- 2. PERFIL ---
    @Transactional(readOnly = true)
    public PerfilResponseDTO obterPerfil() {
        // 🚀 Obtém o ID blindado do Token JWT
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // Vai à base de dados buscar o perfil real garantido pela autenticação
        Utilizador user = repository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));

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