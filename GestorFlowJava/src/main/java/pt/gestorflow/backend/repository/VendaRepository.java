package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.model.EstadoPagamento;
import pt.gestorflow.backend.model.Venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = {
            "cliente",
            "contaBancaria",
            "linhas",
            "linhas.artigo",
            "linhas.taxaIva",
            "linhas.centroCusto",
            "linhas.seccaoHomo"
    })
    Page<Venda> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "cliente",
            "contaBancaria",
            "linhas",
            "linhas.artigo",
            "linhas.taxaIva",
            "linhas.centroCusto",
            "linhas.seccaoHomo"
    })
    Optional<Venda> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // ==========================================
    // -- DASHBOARDS --
    // ==========================================

    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v " +
            "WHERE v.utilizador.id = :userId " +
            "AND v.dataVenda >= :inicio AND v.dataVenda <= :fim")
    BigDecimal totalVendasReais(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(v.totalSemIva), 0) FROM Venda v " +
            "WHERE v.utilizador.id = :userId " +
            "AND v.dataVenda >= :inicio AND v.dataVenda <= :fim")
    BigDecimal totalVendasBase(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @EntityGraph(attributePaths = {"linhas", "linhas.artigo"})
    @Query("SELECT v FROM Venda v " +
            "WHERE v.utilizador.id = :userId " +
            "AND v.dataVenda >= :inicio AND v.dataVenda <= :fim " +
            "ORDER BY v.dataVenda DESC")
    List<Venda> findRecentVendas(@Param("userId") Long userId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId AND v.dataVenda BETWEEN :inicio AND :fim")
    BigDecimal totalVendasPorPeriodo(Long userId, LocalDate inicio, LocalDate fim);

    List<Venda> findAllByUtilizadorIdAndEstadoPagamentoIn(Long utilizadorId, List<EstadoPagamento> estados);
}