package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LinhaVendaDTO {

    @NotNull(message = "O artigo é obrigatório")
    private Long artigoId;

    @NotNull(message = "A taxa de IVA é obrigatória")
    private Long taxaIvaId;

    @NotNull
    @Positive(message = "A quantidade tem de ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "O preço unitário é obrigatório")
    private BigDecimal precoUnitario; // Preço base sem IVA

    // 🚀 A Contabilidade Analítica mudou-se para aqui (como no V14 do Flyway)
    private Long centroCustoId;
    private Long seccaoHomoId;

    private String designacaoPersonalizada;
}