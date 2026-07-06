package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpresaDTO {

    @NotBlank(message = "O Nome Fiscal é obrigatório.")
    @Size(max = 255, message = "O Nome Fiscal não pode ter mais de 255 caracteres.")
    private String nomeFiscal;

    @NotBlank(message = "O NIF é obrigatório.")
    @Size(min = 9, max = 9, message = "O NIF português tem de ter exatamente 9 dígitos.")
    @Pattern(regexp = "\\d{9}", message = "O NIF deve conter apenas números.")
    private String nif;

    @NotBlank(message = "A Morada é obrigatória.")
    private String moradaCompleta;

    @NotBlank(message = "O Código Postal é obrigatório.")
    @Size(max = 20)
    private String codigoPostal;

    @NotBlank(message = "A Localidade é obrigatória.")
    @Size(max = 100)
    private String localidade;

    @NotBlank(message = "O Telefone é obrigatório.")
    @Size(max = 50)
    private String telefone;

    @NotBlank(message = "O Email Geral é obrigatório.")
    @Email(message = "O Email tem de ser válido.")
    @Size(max = 100)
    private String emailGeral;

    @Size(max = 500)
    private String logotipoPath;
}