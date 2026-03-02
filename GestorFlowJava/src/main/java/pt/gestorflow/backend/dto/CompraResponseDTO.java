package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CompraResponseDTO {
    private Long id;
    private LocalDateTime dataCompra;
    private String numeroFaturaFornecedor;
    private String designacao;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal total;

    // --- Flat Fields (Campos Planos) ---
    private Long fornecedorId;
    private String fornecedorNome;

    private Long artigoId;
    private String artigoNome;

    private Long centroCustoId;
    private String centroCustoCodigo;

    private Long seccaoHomoId;
    private String seccaoHomoCodigo;

    private Long taxaIvaId;
    private BigDecimal taxaIvaValor;

    private Long contaBancariaId;
    private String contaBancariaNome;
}