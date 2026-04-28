package pt.gestorflow.backend.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;
import pt.gestorflow.backend.service.AuthService;

import java.util.Optional;

@Component
// 🚀 AQUI ESTÁ A MAGIA: O sistema de auditoria passa a devolver uma String (Texto)
public class AuditorAwareImpl implements AuditorAware<String> {

    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    public AuditorAwareImpl(UtilizadorRepository utilizadorRepository, AuthService authService) {
        this.utilizadorRepository = utilizadorRepository;
        this.authService = authService;
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Long userId = authService.getUtilizadorAutenticadoId();

            // 🚀 Vamos à base de dados, mas em vez de devolvermos a entidade inteira,
            // extraímos apenas o Nome do Utilizador (String) para carimbar na auditoria.
            return utilizadorRepository.findById(userId)
                    .map(Utilizador::getNomeUtilizador); // Se preferires o email, muda para getEmail

        } catch (Exception e) {
            // Se falhar a autenticação (ex: comandos automáticos do sistema), fica vazio
            return Optional.empty();
        }
    }
}