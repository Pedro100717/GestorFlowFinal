package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class FornecedorResponseDTO {
    private Long id;
    private String nome;
    private String nif;
    private String email;
    private String telefone;
    private String morada;
    private String website;
}
