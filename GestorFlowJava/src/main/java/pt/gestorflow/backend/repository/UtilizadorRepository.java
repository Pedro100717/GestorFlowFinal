package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Utilizador;

import java.util.Optional;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    Optional<Utilizador> findByNomeUtilizador(String nomeUtilizador);

    Optional<Utilizador> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByNomeUtilizador(String nomeUtilizador);
}
