package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompraResponseDTO {
    private Long id;
    private LocalDate dataCompra;

    // 🚀 O NOVO CAMPO: Para o Angular saber quando a fatura vence
    private LocalDate dataVencimento;
    private LocalDate dataPrevistaPagamento;

    private String numeroFaturaFornecedor;
    private BigDecimal total; // Soma de todas as linhas

    // 🛡️ Estado do Pagamento (PENDENTE, PARCIALMENTE_PAGO, PAGO)
    private String estadoPagamento;

    // --- Flat Fields do Cabeçalho ---
    private Long fornecedorId;
    private String fornecedorNome;

    private Long planoOrigemId;

    // 🛡️ A CORREÇÃO MANTIDA: Campos da Conta Bancária
    private Long contaBancariaId;
    private String contaBancariaNome;

    // 📦 A Lista de Artigos que a fatura contém
    private List<LinhaCompraResponseDTO> linhas;
}