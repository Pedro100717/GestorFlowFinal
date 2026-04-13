package pt.gestorflow.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Importante!
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fornecedores")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório por causa da herança do Auditable
public class Fornecedor extends Auditable { // <--- Liga o motor de auditoria

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String nif;
    private String email;
    private String telefone;
    private String morada;
    private String website;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizador_id", nullable = false)
    @JsonIgnore
    private Utilizador utilizador;

}