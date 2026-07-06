package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PerfilUtilizadorDTO {
    @NotBlank(message = "O nome não pode estar vazio.")
    private String nome;

    @Email(message = "Email inválido.")
    @NotBlank(message = "O email é obrigatório.")
    private String email;
}