package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.AnaliseAnaliticaDTO;
import pt.gestorflow.backend.repository.AnaliseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnaliseService {

    private final AnaliseRepository analiseRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<AnaliseAnaliticaDTO> obterDashboard() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🚀 O MAPEAMENTO INDUSTRIAL: Da Base de Dados (Projection) para o objeto da API (DTO)
        return analiseRepository.obterAnaliseVendasCompras(utilizadorId).stream()
                .map(proj -> {
                    AnaliseAnaliticaDTO dto = new AnaliseAnaliticaDTO();

                    // 🚀 MAPEAMENTO ATUALIZADO: Os 4 campos distintos em vez de 2!
                    dto.setCentroCustoCodigo(proj.getCentroCustoCodigo());
                    dto.setCentroCustoNome(proj.getCentroCustoNome());
                    dto.setSeccaoCodigo(proj.getSeccaoCodigo());
                    dto.setSeccaoNome(proj.getSeccaoNome());

                    // Operacional
                    dto.setTotalVendasSemIva(proj.getTotalVendasSemIva());
                    dto.setTotalComprasSemIva(proj.getTotalComprasSemIva());
                    dto.setMargemBruta(proj.getMargemBruta());

                    // Fiscal
                    dto.setTotalIvaVendas(proj.getTotalIvaVendas());
                    dto.setTotalIvaCompras(proj.getTotalIvaCompras());
                    dto.setSaldoIva(proj.getSaldoIva());

                    return dto;
                })
                .collect(Collectors.toList());
    }
}