package pt.gestorflow.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConfirmarPagamentoDTO {
    private Long documentoId;
    private String tipoDocumento;
    private Long contaBancariaId;
    private LocalDateTime dataPagamento;
}