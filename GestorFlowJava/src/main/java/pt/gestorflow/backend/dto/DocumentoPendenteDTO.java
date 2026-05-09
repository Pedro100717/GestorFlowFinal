package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoPendenteDTO {
    private Long id;
    private String tipo; // "VENDA" ou "COMPRA"
    private LocalDateTime data;
    private String entidade;
    private BigDecimal total;

    // 🚀 O CAMPO OBRIGATÓRIO PARA OS PARCIAIS
    private BigDecimal valorPendente;
}