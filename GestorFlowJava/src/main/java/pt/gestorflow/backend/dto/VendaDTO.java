package pt.gestorflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Data
public class VendaDTO {
    @NotNull(message = "O ID do Cliente é obrigatório")
    private Long clienteId;

    private LocalDate dataVenda;

    // 🚀 O NOVO CAMPO PARA O SIMULADOR DE TESOURARIA
    private LocalDate dataVencimento;

    private Long centroCustoId;
    private Long seccaoHomoId;

    @NotEmpty(message = "A venda deve ter pelo menos uma linha")
    @Valid
    private List<LinhaVendaDTO> linhas;

    @Data
    public static class LinhaVendaDTO {
        @NotNull(message = "O ID do Artigo é obrigatório")
        private Long artigoId;

        @NotNull(message = "A Taxa de IVA é obrigatória")
        private Long taxaIvaId;

        @NotNull(message = "A quantidade é obrigatória")
        private BigDecimal quantidade;

        @NotNull(message = "O Preço Unitário é obrigatório")
        private BigDecimal precoUnitario;

        private String designacaoPersonalizada;
    }
}