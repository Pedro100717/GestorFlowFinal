package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.LinhaCompra;

import java.util.List;

@Repository
public interface LinhaCompraRepository extends JpaRepository<LinhaCompra, Long> {

    // Deixo-te já este método de bónus caso no futuro precises de saber
    // todas as compras onde um determinado artigo foi adquirido
    List<LinhaCompra> findByArtigoId(Long artigoId);
}