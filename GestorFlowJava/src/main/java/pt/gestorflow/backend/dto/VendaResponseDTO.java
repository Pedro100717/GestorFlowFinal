package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VendaResponseDTO {
    private Long id;
    private LocalDateTime dataVenda;
    private String designacao;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal totalSemIva;
    private BigDecimal totalComIva;

    // --- Flat Fields (Campos Planos) ---
    private Long clienteId;
    private String clienteNome;

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