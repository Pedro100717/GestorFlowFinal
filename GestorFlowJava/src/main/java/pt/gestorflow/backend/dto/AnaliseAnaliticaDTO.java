package pt.gestorflow.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnaliseAnaliticaDTO {

    // 🚀 O NOVO CONTRATO: Código e Nome separados!
    private String centroCustoCodigo;
    private String centroCustoNome;

    private String seccaoCodigo;
    private String seccaoNome;

    // Operacional
    private BigDecimal totalVendasSemIva;
    private BigDecimal totalComprasSemIva;
    private BigDecimal margemBruta;

    // Fiscal
    private BigDecimal totalIvaVendas;
    private BigDecimal totalIvaCompras;
    private BigDecimal saldoIva;
}