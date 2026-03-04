package pt.gestorflow.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContaBancariaResponseDTO {
    private Long id;
    private String nome;
    private String iban;
    private BigDecimal saldo;
}
