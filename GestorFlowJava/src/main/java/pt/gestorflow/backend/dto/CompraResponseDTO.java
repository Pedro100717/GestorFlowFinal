package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompraResponseDTO {
    private Long id;
    private LocalDate dataCompra;

    // 🚀 O NOVO CAMPO: Para o Angular saber quando a fatura vence
    private LocalDate dataVencimento;

    private String numeroFaturaFornecedor;
    private String designacao;
    private BigDecimal quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal total;

    // 🛡️ Estado do Pagamento (PENDENTE ou PAGO)
    private String estadoPagamento;

    // --- Flat Fields (Campos Planos) ---
    private Long fornecedorId;
    private String fornecedorNome;

    private Long artigoId;
    private String artigoNome;

    private Long centroCustoId;
    private String centroCustoCodigo;

    private Long seccaoHomoId;
    private String seccaoHomoCodigo;

    private Long planoOrigemId;

    private Long taxaIvaId;
    private BigDecimal taxaIvaValor;

    // 🛡️ A CORREÇÃO: Faltavam os campos da Conta Bancária!
    private Long contaBancariaId;
    private String contaBancariaNome;
}