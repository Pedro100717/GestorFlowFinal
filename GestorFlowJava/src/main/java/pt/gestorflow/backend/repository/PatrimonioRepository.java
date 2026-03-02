package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Patrimonio;

public interface PatrimonioRepository extends JpaRepository<Patrimonio, Long> {

    // Agora devolve uma Página e exige o Pageable!
    Page<Patrimonio> findAllByUtilizadorIdAndAtivoTrue(Long utilizadorId, Pageable pageable);
}