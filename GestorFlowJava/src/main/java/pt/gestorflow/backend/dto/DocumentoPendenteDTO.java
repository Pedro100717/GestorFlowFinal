package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoPendenteDTO {
    private Long id;
    private String tipo;
    private LocalDateTime data;
    private String entidade;
    private BigDecimal total;
    private BigDecimal valorPendente;

    // 🚀 O NOVO CAMPO QUE O ANGULAR ESTÁ À ESPERA!
    private String descricao;
}