package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.Venda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    // O EntityGraph resolve o problema N+1 para as Vendas!
    @EntityGraph(attributePaths = {"cliente", "artigo", "contaBancaria", "centroCusto", "seccaoHomo", "taxaIva"})
    Page<Venda> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId")
    BigDecimal totalVendasReais(Long userId);

    List<Venda> findTop5ByUtilizadorIdOrderByDataVendaDesc(Long utilizadorId);

    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId AND v.dataVenda BETWEEN :inicio AND :fim")
    BigDecimal totalVendasPorPeriodo(Long userId, LocalDateTime inicio, LocalDateTime fim);
}