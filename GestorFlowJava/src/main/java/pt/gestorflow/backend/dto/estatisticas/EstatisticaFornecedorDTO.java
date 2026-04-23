package pt.gestorflow.backend.dto.estatisticas;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EstatisticaFornecedorDTO {
    private Long fornecedorId;
    private String fornecedorNome;
    private BigDecimal totalGasto;
    private String moeda;
}