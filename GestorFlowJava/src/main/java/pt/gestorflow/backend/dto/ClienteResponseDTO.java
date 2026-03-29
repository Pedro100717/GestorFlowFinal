package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class ClienteResponseDTO {
    private Long id;
    private String nome;
    private String nif;
    private String email;
    private String telefone;
    private String morada;
    private String anotacoes;
}
