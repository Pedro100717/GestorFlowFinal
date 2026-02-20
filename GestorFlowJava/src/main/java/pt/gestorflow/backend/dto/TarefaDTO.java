package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pt.gestorflow.backend.model.Tarefa;
import java.time.LocalDate;

@Data
public class TarefaDTO {

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    private String descricao;

    @NotNull(message = "A prioridade é obrigatória")
    private Tarefa.PrioridadeTarefa prioridade;

    private Tarefa.EstadoTarefa estado; // Opcional no input (assume PENDENTE se null)

    private LocalDate dataLimite;

    private Long clienteId; // Opcional
}