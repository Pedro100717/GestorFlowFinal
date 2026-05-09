package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VendaResponseDTO {
    private Long id;
    private String designacao;
    private LocalDateTime dataVenda;

    // 🚀 O NOVO CAMPO: Para o Angular saber quando a fatura vence
    private LocalDateTime dataVencimento;

    private BigDecimal totalSemIva;
    private BigDecimal totalComIva;
    private String estadoPagamento;

    private Long clienteId;
    private String clienteNome;

    // 🛡️ A CORREÇÃO: O campo que faltava para a Conta Movimentada
    private Long contaBancariaId;
    private String contaBancariaNome;

    // 🛡️ Campos da Contabilidade Analítica
    private Long centroCustoId;
    private String centroCustoCodigo;
    private Long seccaoHomoId;
    private String seccaoHomoCodigo;

    private List<LinhaVendaResponseDTO> linhas;

    @Data
    public static class LinhaVendaResponseDTO {
        private Long id;
        private Long artigoId;
        private String artigoNome;
        private BigDecimal quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal totalLinhaSemIva;
        private BigDecimal totalLinhaComIva;
        private BigDecimal taxaIvaValor;
        private String designacaoPersonalizada;
    }
}