package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferenciaDTO {

    @NotNull(message = "A conta de origem é obrigatória")
    private Long contaOrigemId;

    @NotNull(message = "A conta de destino é obrigatória")
    private Long contaDestinoId;

    @NotNull(message = "O valor da transferência é obrigatório")
    @Positive(message = "O valor tem de ser maior que zero")
    private BigDecimal valor;

    private String descricao;
}