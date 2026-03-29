package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class PerfilResponseDTO {
    private Long id;
    private String nomeUtilizador;
    private String email;
}
