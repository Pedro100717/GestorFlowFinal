package pt.gestorflow.backend.dto;

import java.math.BigDecimal;

public interface AnaliseAnaliticaProjection {
    String getCentroCusto();
    String getSeccaoHomo();

    // Operacional
    BigDecimal getTotalVendasSemIva();
    BigDecimal getTotalComprasSemIva();
    BigDecimal getMargemBruta();

    // Fiscal
    BigDecimal getTotalIvaVendas();
    BigDecimal getTotalIvaCompras();
    BigDecimal getSaldoIva();
}