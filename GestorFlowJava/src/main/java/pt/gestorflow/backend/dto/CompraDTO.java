package pt.gestorflow.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompraDTO {

    @NotNull(message = "O fornecedor é obrigatório")
    private Long fornecedorId;

    private String numeroFaturaFornecedor;
    private Long planoOrigemId;

    private LocalDate dataCompra;

    // 🚀 O NOVO CAMPO PARA O SIMULADOR DE TESOURARIA
    private LocalDate dataVencimento;

    // 📦 A Lista de Artigos vindos do FormArray do Angular
    @NotEmpty(message = "A compra tem de ter pelo menos uma linha de artigo")
    @Valid // Diz ao Spring para validar as regras (@NotNull, @Positive) dentro do LinhaCompraDTO
    private List<LinhaCompraDTO> linhas;
}