package pt.gestorflow.backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeccaoHomoDTO {
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O código é obrigatório")
    private String codigo;

    // LÓGICA: Obrigatório indicar a que Centro pertence
    @NotNull(message = "O Centro de Custo é obrigatório")
    private Long centroCustoId;

    // Opcional: Para leitura (enviar o nome do centro para o frontend)
    private String centroCustoNome;
}