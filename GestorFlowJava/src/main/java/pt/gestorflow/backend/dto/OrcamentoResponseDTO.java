package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrcamentoResponseDTO {
    private Long id;
    private LocalDateTime dataCriacao;
    private LocalDate dataValidade;
    private String estado;
    private String notas;

    // Totais
    private BigDecimal totalCusto;
    private BigDecimal totalSemIva;
    private BigDecimal totalComIva;

    // Dados Flat do Cliente (Sem trazer a entidade toda)
    private Long clienteId;
    private String clienteNome;

    // Linhas Flat
    private List<LinhaResponseDTO> linhas;

    @Data
    public static class LinhaResponseDTO {
        private Long id;
        private Long artigoId;
        private String artigoNome;
        private BigDecimal quantidade;
        private BigDecimal precoCustoUnitario;
        private BigDecimal precoVendaUnitario;
        private BigDecimal margemLucroPercentual;
        private BigDecimal totalLinhaSemIva;
        private BigDecimal totalLinhaComIva;
        private Long taxaIvaId;
        private BigDecimal taxaIvaValor;
    }
}