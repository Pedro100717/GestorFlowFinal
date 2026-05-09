package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AnaliseAnaliticaDTO {
    private String centroCusto;
    private String seccaoHomo;

    // Operacional
    private BigDecimal totalVendasSemIva;
    private BigDecimal totalComprasSemIva;
    private BigDecimal margemBruta;

    // Fiscal
    private BigDecimal totalIvaVendas;
    private BigDecimal totalIvaCompras;
    private BigDecimal saldoIva;
}