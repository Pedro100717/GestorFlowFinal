package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VendaDTO {

    @NotNull(message = "O ID do Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "O ID do Artigo é obrigatório")
    private Long artigoId;

    @NotNull(message = "A Taxa de IVA é obrigatória")
    private Long taxaIvaId;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    // --- NOVO CAMPO OBRIGATÓRIO ---
    // Agora o preço é definido na hora da venda, permitindo flexibilidade
    @NotNull(message = "O Preço Unitário é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    private BigDecimal precoUnitario;

    private String designacaoPersonalizada;

    private Long centroCustoId;
    private Long seccaoHomoId;

    private LocalDate dataVenda;
}