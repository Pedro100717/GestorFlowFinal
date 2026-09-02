package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.ChaveIdempotencia;

@Repository
public interface ChaveIdempotenciaRepository extends JpaRepository<ChaveIdempotencia, String> {

    // O JpaRepository já nos dá o save() e o findById() que precisamos de borla
}