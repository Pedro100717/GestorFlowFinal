package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
public class TxIvaResponseDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;
}