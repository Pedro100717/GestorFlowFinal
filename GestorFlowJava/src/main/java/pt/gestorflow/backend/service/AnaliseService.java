package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.AnaliseAnaliticaProjection;
import pt.gestorflow.backend.repository.AnaliseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    private final AnaliseRepository analiseRepository;
    private final AuthService authService; // 🚀 Injeta o nosso segurança

    @Transactional(readOnly = true)
    public List<AnaliseAnaliticaProjection> obterDashboard() {
        // 🚀 Vai buscar o ID blindado
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // A magia acontece aqui: vai buscar os dados já somados da base de dados
        return analiseRepository.obterAnaliseVendasCompras(utilizadorId);
    }
}