package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "utilizadores")
@Getter
@Setter
public class Utilizador extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nomeUtilizador;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private Boolean verificado = false;

    private String codigoVerificacao;

    // ==========================================
    // 🛡️ O VERDADEIRO EQUALS & HASHCODE PARA JPA
    // ==========================================

    @Override
    public boolean equals(Object o) {
        // 1. Se for exatamente o mesmo espaço de memória, é igual.
        if (this == o) return true;

        // 2. Se o outro objeto não for um Utilizador (ou for nulo), é falso.
        if (!(o instanceof Utilizador that)) return false;

        // 3. Só são iguais se o meu ID não for nulo E for igual ao ID do outro.
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        // Devolvemos sempre um valor constante (o hash da classe).
        // Isto garante que o objeto nunca muda de "gaveta" num HashSet
        // quer o ID seja nulo (antes de gravar na BD) quer já tenha um ID (após gravar).
        return getClass().hashCode();
    }
}