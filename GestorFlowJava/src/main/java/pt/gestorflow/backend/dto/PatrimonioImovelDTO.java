package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PatrimonioImovelDTO {
    @NotBlank private String nome;
    private LocalDate dataAquisicao;
    private BigDecimal valorAquisicao;

    @NotBlank(message = "Morada é obrigatória")
    private String morada;
    private String artigoMatricial;
    private String tipo; // Urbano/Rústico
}