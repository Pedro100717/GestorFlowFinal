package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.dto.PerfilUtilizadorDTO;
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
        if (repository.existsByEmail(dados.getEmail()) || repository.existsByNomeUtilizador(dados.getNomeUtilizador())) {
            throw new IllegalArgumentException("Os dados introduzidos são inválidos ou já estão em uso.");
        }

        Utilizador novoUser = new Utilizador();
        novoUser.setNomeUtilizador(dados.getNomeUtilizador());
        novoUser.setEmail(dados.getEmail());
        novoUser.setSenha(passwordEncoder.encode(dados.getSenha()));

        Utilizador guardado = repository.save(novoUser);

        return converterParaDTO(guardado);
    }

    // --- 2. PERFIL (LEITURA) ---
    @Transactional(readOnly = true)
    public PerfilResponseDTO obterPerfil() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Utilizador user = repository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));

        return converterParaDTO(user);
    }

    // --- 3. PERFIL (ATUALIZAÇÃO) 🚀 NOVO MÉTODO ---
    @Transactional
    public PerfilResponseDTO atualizarPerfil(PerfilUtilizadorDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        Utilizador user = repository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));

        // 🛡️ Validação: Garantir que o email/nome novo não choca com o de OUTRO utilizador na BD
        if (!user.getEmail().equals(dto.getEmail()) && repository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Este email já está em uso por outra conta.");
        }
        if (!user.getNomeUtilizador().equals(dto.getNome()) && repository.existsByNomeUtilizador(dto.getNome())) {
            throw new IllegalArgumentException("Este nome de utilizador já está em uso.");
        }

        user.setNomeUtilizador(dto.getNome());
        user.setEmail(dto.getEmail());

        Utilizador guardado = repository.save(user);

        return converterParaDTO(guardado);
    }

    // --- 4. CONVERSOR ---
    private PerfilResponseDTO converterParaDTO(Utilizador user) {
        PerfilResponseDTO dto = new PerfilResponseDTO();
        dto.setId(user.getId());
        dto.setNomeUtilizador(user.getNomeUtilizador());
        dto.setEmail(user.getEmail());
        return dto;
    }
}