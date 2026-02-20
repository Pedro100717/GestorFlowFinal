package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import pt.gestorflow.backend.validation.NifPT;

@Data
public class ClienteDTO {

    // Usado para enviar o ID de volta ao frontend
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NifPT(message = "O NIF inserido não é valido")
    private String nif;

    private String email;
    private String telefone;
    private String morada;
    private String anotacoes;
}