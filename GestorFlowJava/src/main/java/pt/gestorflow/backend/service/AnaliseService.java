package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.AnaliseAnaliticaDTO;
import pt.gestorflow.backend.repository.AnaliseRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 1. O Lombok trata da criação do Logger automaticamente!
@Service
@RequiredArgsConstructor
public class AnaliseService {

    private final AnaliseRepository analiseRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<AnaliseAnaliticaDTO> obterDashboard() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 2. Registo de Auditoria (INFO): Importante para saber quem está a ver as finanças da empresa.
        log.info("A gerar Dashboard Analítico para o utilizador ID: {}", utilizadorId);

        // Extraímos para uma variável para podermos logar o tamanho final antes de devolver
        List<AnaliseAnaliticaDTO> resultados = analiseRepository.obterAnaliseVendasCompras(utilizadorId).stream()
                .map(proj -> {
                    AnaliseAnaliticaDTO dto = new AnaliseAnaliticaDTO();

                    // 🚀 MAPEAMENTO ATUALIZADO
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

        // 3. Registo de Sucesso (DEBUG): Ajuda-te a ver se as queries estão a devolver o que devem, mas não enche os logs em produção a menos que peças.
        log.debug("Dashboard gerado com sucesso para utilizador ID: {}. Total de secções agregadas: {}", utilizadorId, resultados.size());

        return resultados;
    }
}