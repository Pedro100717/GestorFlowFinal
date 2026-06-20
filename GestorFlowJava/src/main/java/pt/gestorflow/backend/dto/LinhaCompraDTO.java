package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LinhaCompraDTO {

    @NotNull(message = "O artigo é obrigatório")
    private Long artigoId;

    @NotNull(message = "A taxa de IVA é obrigatória")
    private Long taxaIvaId;

    @NotNull
    @Positive(message = "A quantidade tem de ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "O preço unitário é obrigatório")
    private BigDecimal precoUnitario; // Preço base sem IVA

    // Contabilidade Analítica agora vive aqui na linha
    private Long centroCustoId;
    private Long seccaoHomoId;

    private String designacaoPersonalizada;
}