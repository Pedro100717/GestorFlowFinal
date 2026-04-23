package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class SeccaoHomoResponseDTO {
    private Long id;
    private String nome;
    private String codigo;

    //Flat Fields frontend tem que saber a que cc isto pertence
    private String centroCustoNome;
}
