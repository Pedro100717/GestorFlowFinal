package pt.gestorflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class VendaDTO {

    @NotNull(message = "O cliente é obrigatório")
    private Long clienteId;

    private LocalDate dataVenda;

    // 🚀 O MOTOR PARA O SIMULADOR DE TESOURARIA
    private LocalDate dataVencimento;

    // 🚀 O ELO SECRETO DA TESOURARIA
    private Long planoOrigemId;

    // 📦 A Lista de Artigos vindos do FormArray do Angular
    @NotEmpty(message = "A venda tem de ter pelo menos uma linha de artigo")
    @Valid // Garante que as anotações dentro do LinhaVendaDTO são respeitadas
    private List<LinhaVendaDTO> linhas;
}