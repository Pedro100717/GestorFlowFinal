package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chaves_idempotencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChaveIdempotencia {

    // A própria chave gerada pelo Angular será o ID (a chave primária)
    @Id
    @Column(length = 36, nullable = false, unique = true)
    private String chave;

    @Column(nullable = false)
    private Long utilizadorId;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;
}