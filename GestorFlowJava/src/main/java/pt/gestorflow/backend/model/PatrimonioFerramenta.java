package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patrimonio_ferramentas")
@Getter
@Setter
public class PatrimonioFerramenta extends Patrimonio {

    // 🛡️ Dica Industrial: O número de série costuma ser um excelente candidato para pesquisas,
    // limitar o tamanho ajuda na indexação da base de dados.
    @Column(length = 100)
    private String numeroSerie;

    @Column(length = 50)
    private String estadoConservacao;

    // 🛡️ REMOVIDO: @EqualsAndHashCode(callSuper = true)
    // A classe já herda o equals() e hashCode() perfeitos da classe Patrimonio (baseados no ID).
    // Se o Lombok gerasse isto aqui, ia incluir o "estadoConservacao" no cálculo.
    // Ou seja, se reparasses uma ferramenta (mudando o estado para "Novo"),
    // ela "desaparecia" das listas em memória do Hibernate!
}