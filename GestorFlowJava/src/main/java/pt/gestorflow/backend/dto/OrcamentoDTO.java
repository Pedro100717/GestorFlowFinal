package pt.gestorflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrcamentoDTO {
    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    private LocalDate dataValidade;
    private String notas;

    @NotEmpty(message = "O orçamento tem de ter pelo menos uma linha")
    @Valid
    private List<LinhaOrcamentoDTO> linhas;

    @Data
    public static class LinhaOrcamentoDTO {
        @NotNull private Long artigoId;
        @NotNull private Long taxaIvaId;
        @NotNull @Positive private BigDecimal quantidade;

        // O utilizador pode escolher preencher a margem (%) OU o preço de venda final direto
        private BigDecimal margemLucroPercentual;
        private BigDecimal precoVendaUnitarioOverride;
    }
}