package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.dto.PerfilUtilizadorDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class UtilizadorService {

    private final UtilizadorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    // --- 1. REGISTO ---
    @Transactional
    public PerfilResponseDTO registarNovoUtilizador(RegistoDTO dados) {
        log.info("A processar pedido de registo de novo utilizador (Email providenciado: {})", dados.getEmail());

        if (repository.existsByEmail(dados.getEmail()) || repository.existsByNomeUtilizador(dados.getNomeUtilizador())) {
            // 🛡️ WARN: Monitorização de possíveis ataques de enumeração ou simples enganos
            log.warn("Tentativa de registo bloqueada: Os dados já se encontram em uso (Email: {} / Username: {})", dados.getEmail(), dados.getNomeUtilizador());
            throw new IllegalArgumentException("Os dados introduzidos são inválidos ou já estão em uso.");
        }

        Utilizador novoUser = new Utilizador();
        novoUser.setNomeUtilizador(dados.getNomeUtilizador());
        novoUser.setEmail(dados.getEmail());
        novoUser.setSenha(passwordEncoder.encode(dados.getSenha()));

        Utilizador guardado = repository.save(novoUser);

        log.info("Auditoria de Segurança: Novo utilizador criado com sucesso. ID: {}", guardado.getId());

        return converterParaDTO(guardado);
    }

    // --- 2. PERFIL (LEITURA) ---
    @Transactional(readOnly = true)
    public PerfilResponseDTO obterPerfil() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Acesso aos dados de perfil solicitado pelo utilizador ID: {}", utilizadorId);

        Utilizador user = repository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));

        return converterParaDTO(user);
    }

    // --- 3. PERFIL (ATUALIZAÇÃO) ---
    @Transactional
    public PerfilResponseDTO atualizarPerfil(PerfilUtilizadorDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria de Segurança: Pedido de atualização de perfil iniciado pelo utilizador ID: {}", utilizadorId);

        Utilizador user = repository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado no sistema."));

        if (!user.getEmail().equals(dto.getEmail()) && repository.existsByEmail(dto.getEmail())) {
            log.warn("Bloqueada tentativa do utilizador ID: {} de alterar o email para um já existente ({})", utilizadorId, dto.getEmail());
            throw new IllegalArgumentException("Este email já está em uso por outra conta.");
        }

        if (!user.getNomeUtilizador().equals(dto.getNome()) && repository.existsByNomeUtilizador(dto.getNome())) {
            log.warn("Bloqueada tentativa do utilizador ID: {} de alterar o username para um já existente ({})", utilizadorId, dto.getNome());
            throw new IllegalArgumentException("Este nome de utilizador já está em uso.");
        }

        boolean emailAlterado = !user.getEmail().equals(dto.getEmail());

        user.setNomeUtilizador(dto.getNome());
        user.setEmail(dto.getEmail());

        Utilizador guardado = repository.save(user);

        if (emailAlterado) {
            log.info("ALERTA DE SEGURANÇA: O utilizador ID: {} alterou o seu email principal para: {}", utilizadorId, dto.getEmail());
        } else {
            log.debug("Perfil do utilizador ID: {} atualizado com sucesso (sem alteração de email).", utilizadorId);
        }

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