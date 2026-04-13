package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Não esquecer o import!
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seccao_homo")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para a herança funcionar corretamente
public class SeccaoHomo extends Auditable { // <--- Motor de Auditoria ligado

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String codigo;

    // --- RELAÇÃO NOVA: Uma secção pertence a um Centro de Custo ---
    @ManyToOne
    @JoinColumn(name = "centro_custo_id", nullable = false)
    private CentroCusto centroCusto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;
}