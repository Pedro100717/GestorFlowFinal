package pt.gestorflow.backend.dto;

import java.math.BigDecimal;

public interface AnaliseAnaliticaProjection {

    // 🚀 O NOVO CONTRATO: Campos separados!
    String getCentroCustoCodigo();
    String getCentroCustoNome();

    String getSeccaoCodigo();
    String getSeccaoNome();

    // Operacional
    BigDecimal getTotalVendasSemIva();
    BigDecimal getTotalComprasSemIva();
    BigDecimal getMargemBruta();

    // Fiscal
    BigDecimal getTotalIvaVendas();
    BigDecimal getTotalIvaCompras();
    BigDecimal getSaldoIva();
}