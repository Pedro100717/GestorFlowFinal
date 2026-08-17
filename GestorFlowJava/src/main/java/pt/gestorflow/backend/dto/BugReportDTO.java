package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class BugReportDTO {
    private String tipo;
    private String descricao;
    private String paginaOrigem;
    private String emailUtilizador;
}