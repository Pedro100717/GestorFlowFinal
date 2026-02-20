package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PatrimonioViaturaDTO {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    private LocalDate dataAquisicao;
    private BigDecimal valorAquisicao;

    @NotBlank(message = "Matrícula é obrigatória")
    private String matricula;
    private String marca;
    private String modelo;
    private LocalDate validadeSeguro;
    private LocalDate proximaInspecao;
}