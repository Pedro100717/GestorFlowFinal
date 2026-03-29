package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PatrimonioImovelDTO {
    @NotBlank private String nome;

    @NotNull(message = "A data de aquisição é obrigatória")
    private LocalDate dataAquisicao;

    @NotNull(message = "O valor de aquisição é obrigatório")
    @PositiveOrZero(message = "O valor não pode ser negativo")
    private BigDecimal valorAquisicao;

    @NotBlank(message = "Morada é obrigatória")
    private String morada;
    private String artigoMatricial;
    private String tipo; // Urbano/Rústico
}