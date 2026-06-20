package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistoDTO {

    @NotBlank(message = "O nome de utilizador é obrigatorio")
    @Size(min = 3, max = 50, message = "O nome deve ter entre 3 e 50 caracteres")
    private String nomeUtilizador;

    @NotBlank(message = "O email é obrigatorio")
    @Email(message = "O formato do email é invalido")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    // Explicação da Regex Atualizada:
    // (?=.*[0-9]) -> Pelo menos um número
    // (?=.*[A-Z]) -> Pelo menos uma maiúscula
    // .{8,} -> No mínimo 8 caracteres
    // Permitimos espaços e caracteres especiais livremente para evitar erros de encoding no frontend
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z]).{8,}$",
            message = "A senha deve ser forte: mínimo de 8 caracteres, contendo pelo menos 1 letra maiúscula e 1 número")
    private String senha;
}