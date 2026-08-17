package pt.gestorflow.backend.dto;

import lombok.Data;

@Data
public class TicketResponseDTO {
    private Long id;
    private String tipo;
    private String descricao;
    private String paginaOrigem;
    private String emailUtilizador;
    private String nomeUtilizador; // Já mandamos o nome limpinho
}