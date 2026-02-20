package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import pt.gestorflow.backend.validation.NifPT; // A tua anotação personalizada

@Data
public class FornecedorDTO {

    // Não precisamos de ID na criação

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NifPT(message = "NIF inválido")
    private String nif;

    @Email(message = "Formato de email inválido")
    private String email;

    private String telefone;
    private String morada;
    private String website;
}