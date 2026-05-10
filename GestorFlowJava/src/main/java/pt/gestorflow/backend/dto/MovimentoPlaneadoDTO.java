package pt.gestorflow.backend.dto;

import lombok.Getter;
import lombok.Setter;
import pt.gestorflow.backend.model.FrequenciaMovimento;
import pt.gestorflow.backend.model.TipoMovimentoPlaneado;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class MovimentoPlaneadoDTO {

    private Long id; // Nulo na criação, preenchido na edição
    private String descricao;

    private TipoMovimentoPlaneado tipo;
    private FrequenciaMovimento frequencia;

    private BigDecimal valorBase;
    private BigDecimal taxaIva;

    private LocalDate dataInicio;
    private LocalDate dataFim; // Opcional

    // 🚀 IDs das dimensões analíticas (Obrigatórios)
    private Long centroCustoId;
    private Long seccaoHomoId;

    // 🚀 IDs dos parceiros (Opcionais)
    private Long clienteId;
    private Long fornecedorId;

    private Boolean ativo;
}