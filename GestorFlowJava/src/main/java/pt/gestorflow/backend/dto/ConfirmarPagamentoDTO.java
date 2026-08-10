package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern; // 🚀 Importação do Pattern
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConfirmarPagamentoDTO {

    @NotNull(message = "O ID do documento é obrigatório.")
    private Long documentoId;

    // 🚀 BLINDAGEM ESTRETA: Só aceita as palavras exatas "COMPRA" ou "VENDA"
    @NotBlank(message = "O tipo de documento é obrigatório.")
    @Pattern(regexp = "^(COMPRA|VENDA)$", message = "O tipo de documento tem de ser estritamente 'COMPRA' ou 'VENDA'.")
    private String tipoDocumento;

    @NotNull(message = "A conta bancária é obrigatória para a liquidação.")
    private Long contaBancariaId;

    // 🚀 O CAMPO ESTRELA DOS PAGAMENTOS PARCIAIS
    @NotNull(message = "O valor a pagar é obrigatório.")
    @Positive(message = "O valor a pagar tem de ser estritamente maior que zero.")
    private BigDecimal valorAPagar;

    // Mantemos sem validação, porque no teu Service tens lógica para assumir o "agora" se vier a null
    private LocalDate dataPagamento;
}