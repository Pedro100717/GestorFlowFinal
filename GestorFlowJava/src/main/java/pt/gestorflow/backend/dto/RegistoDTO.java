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
    // Explicação da Regex:
    // (?=.*[0-9]) -> Pelo menos um número
    // (?=.*[a-z]) -> Pelo menos uma minúscula
    // (?=.*[A-Z]) -> Pelo menos uma maiúscula
    // (?=.*[@#$%^&+=!]) -> Pelo menos um caracter especial
    // (?=\S+$) -> Sem espaços em branco
    // .{8,} -> No mínimo 8 caracteres
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "A senha deve ser forte: min 8 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 especial")
    private String senha;
}

