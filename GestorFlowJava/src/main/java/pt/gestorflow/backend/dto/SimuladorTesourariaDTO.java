package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor // 🚀 Obrigatório: Sem isto o Spring/Jackson não consegue processar o JSON!
public class SimuladorTesourariaDTO {

    private BigDecimal saldoAtual;
    private List<PontoSimulacao> pontos;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor // 🚀 Obrigatório: Também precisa de estar na classe aninhada
    public static class PontoSimulacao {
        private String label;
        private BigDecimal saldoProjetado; // O nome agora está perfeito!
    }
}