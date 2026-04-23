package pt.gestorflow.backend.dto.estatisticas;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class EstatisticaClienteDTO {
    private Long clienteId;
    private String clienteNome;
    private BigDecimal totalRecebido;
    private String moeda;
}