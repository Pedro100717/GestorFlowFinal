package pt.gestorflow.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PatrimonioResponseDTO {
    private Long id;
    private String nome;
    private LocalDate dataAquisicao;
    private BigDecimal valorAquisicao;

    // 🛡️ O campo que faltava para o mapToDTO funcionar!
    private boolean ativo;

    // O tipo para o Angular saber o que é (VIATURA, IMOVEL, FERRAMENTA)
    private String tipoPatrimonio;

    // Campos de Viatura
    private String matricula;
    private String marca;
    private String modelo;
    private LocalDate validadeSeguro;
    private LocalDate proximaInspecao;

    // Campos de Imóvel
    private String morada;
    private String artigoMatricial;
    private String tipo; // Urbano/Rústico

    // Campos de Ferramenta
    private String numeroSerie;
    private String estadoConservacao;
}