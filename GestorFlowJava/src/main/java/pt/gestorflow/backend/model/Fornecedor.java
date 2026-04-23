package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fornecedores")
@Getter
@Setter
public class Fornecedor extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 20)
    private String nif;

    private String email;
    private String telefone;
    private String morada;
    private String website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    // 🛡️ Jackson removido: A entidade agora é puramente JPA.
    private Utilizador utilizador;

    // 🛡️ IMPLEMENTAÇÃO SEGURA DE EQUALS E HASHCODE PARA JPA
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fornecedor that)) return false;
        // Compara apenas o ID. Se o ID for nulo, são objetos diferentes em memória.
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        // Retornar o hash da classe garante estabilidade em coleções (Sets/Lists)
        // quer o objeto esteja persistido (com ID) ou seja novo (ID null).
        return getClass().hashCode();
    }
}