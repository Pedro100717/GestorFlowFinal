package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("SERVICO")
@Getter
@Setter
public class Servico extends Artigo {

    // 🛡️ Exemplo prático: Se adicionares colunas específicas no futuro,
    // não te esqueças de lhes dar um nome ou limite claro.
    @Column(name = "duracao_minutos_estimada")
    private Integer duracaoMinutosEstimada;

    // 🛡️ REMOVIDO: @EqualsAndHashCode(callSuper = true) e @Data
    // A classe Artigo (pai) já tem o equals() e hashCode() blindados pelo ID.
    // Esta classe herda essa inteligência nativamente.
}