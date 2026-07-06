package pt.gestorflow.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder // Ajuda a criar o objeto de forma limpa no Service
public class DashboardDTO {
    private BigDecimal totalVendas;
    private BigDecimal margemBruta;
    private BigDecimal valorStock;
    private BigDecimal totalCompras;
    private List<VendaResponseDTO> ultimasVendas;
}