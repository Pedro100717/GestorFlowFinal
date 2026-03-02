package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    //Vai buscar tudo numa so Query
    @EntityGraph(attributePaths = {"fornecedor", "artigo", "contaBancaria", "centroCusto", "seccaoHomo", "taxaIva"})
    Page<Compra> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    //Soma total de compras da vida inteira
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.utilizador.id = :userId")
    BigDecimal totalGastos(Long userId);
}