package pt.gestorflow.backend.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ContaCorrenteExtratoProjection {
    LocalDateTime getDataMovimento();
    String getTipoDocumento();
    String getDescricao();
    BigDecimal getDebito();
    BigDecimal getCredito();
}