package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "utilizadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para fundir com o Auditable
public class Utilizador extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nomeUtilizador;

    @Column(nullable = false)
    private String senha; // Hash da password

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private Boolean verificado = false;

    private String codigoVerificacao;
}