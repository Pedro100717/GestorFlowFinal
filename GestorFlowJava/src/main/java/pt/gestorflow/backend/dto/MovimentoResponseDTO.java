package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MovimentoResponseDTO {
    private Long id;
    private String dataMovimento;
    private String descricao;
    private String tipo; // "CREDITO" ou "DEBITO"
    private BigDecimal valor;

    // Indicadores para os badges/selos (Se for null, o Angular não mostra o selo)
    private Long compraId;
    private Long vendaId;

    private String fornecedorNome; // Para o Angular saber a quem se pagou
    private String clienteNome;    // Para o Angular saber de quem se recebeu
}