package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import pt.gestorflow.backend.model.Movimento;
import java.math.BigDecimal;

@Data
public class MovimentoDTO {
    @NotNull(message = "A conta bancária é obrigatória")
    private Long contaId;

    @NotBlank(message = "A descrição é obrigatória")
    private String descricao;

    @NotNull(message = "O tipo (CREDITO/DEBITO) é obrigatório")
    private Movimento.TipoMovimento tipo;

    @NotNull
    @Positive(message = "O valor deve ser positivo")
    private BigDecimal valor;

    private Long clienteId;
    private Long fornecedorId;
}