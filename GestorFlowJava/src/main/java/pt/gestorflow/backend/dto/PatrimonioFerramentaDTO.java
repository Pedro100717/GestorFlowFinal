package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PatrimonioFerramentaDTO {
    @NotBlank private String nome;
    private LocalDate dataAquisicao;
    private BigDecimal valorAquisicao;

    private String numeroSerie;
    private String estadoConservacao;
}