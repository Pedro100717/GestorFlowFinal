package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // 🛡️ Dica Industrial: Mesmo validando no Service, ter um índice no NIF
    // ajuda muito na performance de procura quando a base de dados crescer.
    @Column(length = 20)
    private String nif;

    private String email;

    private String telefone;

    @Column(columnDefinition = "TEXT")
    private String morada;

    @Column(columnDefinition = "TEXT")
    private String anotacoes;

    // 🚀 Performance: FetchType.LAZY é obrigatório para evitar o problema N+1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido: @JsonIgnore já não é necessário porque a entidade não sai para o controller.
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    // Substitui o @EqualsAndHashCode(callSuper = true) para evitar queries recursivas
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente cliente)) return false;
        return id != null && id.equals(cliente.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}