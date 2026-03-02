package pt.gestorflow.backend.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MovimentoStockResponseDTO {

    private Long id;
    private LocalDateTime dataMovimento;
    private String tipo;
    private BigDecimal quantidade;
    private String motivo;
    private BigDecimal stockAposMovimento;

    private Long mercadoriaId;
    private String mercadoriaNome;
}
