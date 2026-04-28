package pt.gestorflow.backend.dto.projection;

import java.math.BigDecimal;

@SuppressWarnings("unused")
public interface ContaCorrenteFornecedorResumoProjection {
    Long getFornecedorId();
    String getNomeFornecedor();
    BigDecimal getTotalComprado();
    BigDecimal getTotalPago();
    BigDecimal getSaldoPendente();
}