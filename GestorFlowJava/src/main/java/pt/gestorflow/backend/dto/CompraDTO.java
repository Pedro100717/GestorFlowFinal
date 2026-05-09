package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompraDTO {
    @NotNull private Long fornecedorId;
    @NotNull private Long artigoId;

    @NotNull(message = "A taxa de IVA é obrigatória")
    private Long taxaIvaId;

    @NotNull @Positive
    private BigDecimal quantidade;

    @NotNull
    private BigDecimal precoUnitario; // Preço sem IVA

    private String numeroFaturaFornecedor;
    private String designacaoPersonalizada;

    private Long centroCustoId;
    private Long seccaoHomoId;

    private LocalDate dataCompra;

    // 🚀 O NOVO CAMPO PARA O SIMULADOR DE TESOURARIA
    private LocalDate dataVencimento;
}