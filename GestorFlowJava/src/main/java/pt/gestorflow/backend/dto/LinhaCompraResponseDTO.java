package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LinhaCompraResponseDTO {
    private Long id;

    // Dados do Artigo
    private Long artigoId;
    private String artigoNome;

    // Matemática
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal totalLinha;

    // IVA
    private Long taxaIvaId;
    private BigDecimal taxaIvaValor;

    // Analítica
    private Long centroCustoId;
    private String centroCustoCodigo;
    private String centroCustoNome;

    private Long seccaoHomoId;
    private String seccaoHomoCodigo;
    private String seccaoHomoNome;

    private String designacaoPersonalizada;
}