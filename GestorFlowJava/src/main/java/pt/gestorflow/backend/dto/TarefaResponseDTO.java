package pt.gestorflow.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TarefaResponseDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private String prioridade;
    private String estado;
    private LocalDate dataLimite;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataConclusao;

    private Long clienteId;
    private String clienteNome;
}
