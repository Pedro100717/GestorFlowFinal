package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import pt.gestorflow.backend.model.MovimentoStock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MovimentoStockDTO {

    @NotNull(message = "O artigo (mercadoria) é obrigatório")
    private Long mercadoriaId;

    @NotNull(message = "O tipo de movimento (ENTRADA/SAIDA) é obrigatório")
    private MovimentoStock.TipoMovimentoStock tipo;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade tem de ser maior que zero")
    private BigDecimal quantidade;

    @NotBlank(message = "O motivo do acerto é obrigatório")
    private String motivo;

    private LocalDateTime dataMovimento; // Opcional, se null assume a data/hora atual
}