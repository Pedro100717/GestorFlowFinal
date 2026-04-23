package pt.gestorflow.backend.dto;

import java.math.BigDecimal;

public interface AnaliseAnaliticaProjection {
    String getCentroCusto();
    String getSeccaoHomo();
    BigDecimal getTotalVendas();
    BigDecimal getTotalCompras();
    BigDecimal getMargem();
}
