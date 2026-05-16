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

    private Long id;
    private String descricao;

    private TipoMovimentoPlaneado tipo; // ENTRADA ou SAIDA
    private FrequenciaMovimento frequencia; // MENSAL, SEMANAL, etc.

    private Long clienteId;
    private Long fornecedorId;

    private BigDecimal valorBase;

    private LocalDate dataInicio;
    private LocalDate dataFim;

    private Boolean ativo;

    // 🚀 Para o Frontend saber quando o botão deve estar desativado
    private LocalDate dataUltimoProcessamento;
}