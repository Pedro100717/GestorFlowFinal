package pt.gestorflow.backend.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CentroCustoDTO {
    private Long id;
    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    private String codigo;
}