package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.TxIva;

public interface TxIvaRepository extends JpaRepository<TxIva, Long> {
}
