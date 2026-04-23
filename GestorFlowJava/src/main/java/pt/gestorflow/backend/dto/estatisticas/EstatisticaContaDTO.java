package pt.gestorflow.backend.dto.estatisticas;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EstatisticaContaDTO {
    private Long contaId;
    private String contaNome;
    private BigDecimal lucroReal;
    private String moeda; // Ex: "EUR"
}