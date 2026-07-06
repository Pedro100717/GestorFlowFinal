package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresas")
@Getter
@Setter
public class Empresa extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nomeFiscal;

    @Column(nullable = false, length = 20)
    private String nif;

    @Column(columnDefinition = "TEXT")
    private String moradaCompleta;

    @Column(length = 20)
    private String codigoPostal;

    @Column(length = 100)
    private String localidade;

    @Column(length = 50)
    private String telefone;

    @Column(length = 100)
    private String emailGeral;

    @Column(length = 500)
    private String logotipoPath;

    @Column(length = 50)
    private String fusoHorario = "Europe/Lisbon";

    @Column(length = 10)
    private String moedaPadrao = "EUR";

    // 🚀 LIGAÇÃO BLINDADA AO UTILIZADOR LOGADO
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false, unique = true)
    private Utilizador utilizador;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empresa that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}