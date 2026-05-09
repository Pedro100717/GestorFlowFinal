package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.EstadoPagamento;
import pt.gestorflow.backend.model.Venda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    // 🚀 OTIMIZADO: Traz o cliente, conta e as linhas todas de uma vez! (Zero N+1)
    @EntityGraph(attributePaths = {"cliente", "contaBancaria", "linhas.artigo", "linhas.taxaIva"})
    Page<Venda> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // 🛡️ Segurança IDOR para ver os detalhes de uma fatura específica
    @EntityGraph(attributePaths = {"cliente", "contaBancaria", "linhas.artigo", "linhas.taxaIva"})
    Optional<Venda> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // -- DASHBOARDS --
    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId")
    BigDecimal totalVendasReais(Long userId);

    List<Venda> findTop5ByUtilizadorIdOrderByDataVendaDesc(Long utilizadorId);

    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId AND v.dataVenda BETWEEN :inicio AND :fim")
    BigDecimal totalVendasPorPeriodo(Long userId, LocalDateTime inicio, LocalDateTime fim);

    // 🚀 A MUDANÇA INDUSTRIAL FINAL: 'In' no nome do método para suportar PENDENTE e PARCIALMENTE_PAGO
    List<Venda> findAllByUtilizadorIdAndEstadoPagamentoIn(Long utilizadorId, List<EstadoPagamento> estados);
}