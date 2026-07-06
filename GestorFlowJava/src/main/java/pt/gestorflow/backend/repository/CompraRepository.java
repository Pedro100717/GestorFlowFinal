package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.model.Compra;
import pt.gestorflow.backend.model.EstadoPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    @EntityGraph(attributePaths = {
            "fornecedor",
            "contaBancaria",
            "linhas",
            "linhas.artigo",
            "linhas.taxaIva",
            "linhas.centroCusto",
            "linhas.seccaoHomo"
    })
    Optional<Compra> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    @EntityGraph(attributePaths = {
            "fornecedor",
            "contaBancaria",
            "linhas",
            "linhas.artigo",
            "linhas.taxaIva",
            "linhas.centroCusto",
            "linhas.seccaoHomo"
    })
    Page<Compra> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c " +
            "WHERE c.utilizador.id = :userId " +
            "AND c.dataCompra >= :inicio AND c.dataCompra <= :fim")
    BigDecimal totalGastos(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(l.quantidade * l.precoUnitario), 0) " +
            "FROM Compra c JOIN c.linhas l " +
            "WHERE c.utilizador.id = :userId " +
            "AND c.dataCompra >= :inicio AND c.dataCompra <= :fim")
    BigDecimal totalComprasBase(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @EntityGraph(attributePaths = {"linhas", "linhas.artigo"})
    @Query("SELECT c FROM Compra c " +
            "WHERE c.utilizador.id = :userId " +
            "AND c.dataCompra >= :inicio AND c.dataCompra <= :fim " +
            "ORDER BY c.dataCompra DESC")
    List<Compra> findRecentCompras(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, Pageable pageable);

    List<Compra> findAllByUtilizadorIdAndEstadoPagamentoIn(Long utilizadorId, List<EstadoPagamento> estados);
}