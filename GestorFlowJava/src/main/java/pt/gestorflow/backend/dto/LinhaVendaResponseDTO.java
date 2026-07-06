package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LinhaVendaResponseDTO {
    private Long id;

    // Artigo
    private Long artigoId;
    private String artigoNome;

    // Matemática
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal totalLinhaSemIva;
    private BigDecimal totalLinhaComIva;

    // IVA
    private Long taxaIvaId;
    private BigDecimal taxaIvaValor;

    private String designacaoPersonalizada;

    // 🚀 Contabilidade Analítica - Agora com Códigos e Nomes!
    private Long centroCustoId;
    private String centroCustoCodigo;
    private String centroCustoNome; // <-- NOVO

    private Long seccaoHomoId;
    private String seccaoHomoCodigo;
    private String seccaoHomoNome; // <-- NOVO
}