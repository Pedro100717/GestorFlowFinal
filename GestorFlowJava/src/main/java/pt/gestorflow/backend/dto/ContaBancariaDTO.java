package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import pt.gestorflow.backend.validation.IbanPT; // A tua anotação personalizada
import java.math.BigDecimal;

@Data
public class ContaBancariaDTO {

    @NotBlank(message = "O nome da conta é obrigatório")
    private String nome;

    @IbanPT // Valida se começa por PT50 e tem o tamanho certo
    private String iban;

    // Opcional: Se vier null, assumimos 0
    private BigDecimal saldoInicial;
}