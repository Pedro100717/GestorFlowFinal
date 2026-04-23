package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ArtigoResponseDTO {
    private Long id;
    private String nome;
    private String codigoBarras;
    private BigDecimal preco;
    private BigDecimal ultimoPrecoCusto;
    private String tipo; // "MERCADORIA" ou "SERVICO"

    // 🛡️ O CAMPO QUE FALTAVA:
    private boolean movimentaStock;

    private BigDecimal stockAtual;
    private Long familiaId;
    private String familiaNome;
}