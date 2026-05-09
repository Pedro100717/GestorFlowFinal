package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.LinhaVenda;

public interface LinhaVendaRepository extends JpaRepository<LinhaVenda, Long> {
}