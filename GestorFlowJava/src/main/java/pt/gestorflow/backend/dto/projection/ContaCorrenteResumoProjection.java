package pt.gestorflow.backend.dto.projection;

import java.math.BigDecimal;

public interface ContaCorrenteResumoProjection {
    Long getClienteId();
    String getNomeCliente();
    BigDecimal getTotalFaturado();
    BigDecimal getTotalPago();
    BigDecimal getSaldoPendente();

}
