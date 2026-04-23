package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DocumentoPendenteDTO {
    private Long id;
    private String tipo; // "VENDA" ou "COMPRA"
    private LocalDateTime data;
    private String entidade;
    private BigDecimal total;
}