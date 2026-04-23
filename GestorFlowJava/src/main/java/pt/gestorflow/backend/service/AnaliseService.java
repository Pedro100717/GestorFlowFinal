package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.AnaliseAnaliticaProjection;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.AnaliseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    private final AnaliseRepository analiseRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional(readOnly = true)
    public List<AnaliseAnaliticaProjection> obterDashboard() {
        Utilizador user = getUtilizadorLogado();

        // A magia acontece aqui: vai buscar os dados já somados da base de dados
        return analiseRepository.obterAnaliseVendasCompras(user.getId());
    }
}