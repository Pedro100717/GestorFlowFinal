package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class VendaResponseDTO {
    private Long id;
    private LocalDate dataVenda;

    // 🚀 O NOVO CAMPO: Para o Angular saber quando a fatura vence
    private LocalDate dataVencimento;
    private LocalDate dataPrevistaPagamento;

    // 🚀 RASTREABILIDADE: O elo que liga esta venda ao planeamento original
    private Long planoOrigemId;

    private BigDecimal totalSemIva;
    private BigDecimal totalComIva;
    private String estadoPagamento;

    private Long clienteId;
    private String clienteNome;

    // 🛡️ A CORREÇÃO MANTIDA: A Conta Movimentada
    private Long contaBancariaId;
    private String contaBancariaNome;

    // 📦 A Lista de Artigos e Serviços que a fatura contém
    private List<LinhaVendaResponseDTO> linhas;
}