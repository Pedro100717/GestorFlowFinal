package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.Venda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    // Listagem paginada para a tabela de vendas
    Page<Venda> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // DASHBOARD: Total de Vendas (Soma acumulada de sempre)
    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId")
    BigDecimal totalVendasReais(Long userId);

    // DASHBOARD: As 5 vendas mais recentes deste utilizador
    List<Venda> findTop5ByUtilizadorIdOrderByDataVendaDesc(Long utilizadorId);

    // GRÁFICOS: Soma total de vendas por intervalo de datas
    @Query("SELECT COALESCE(SUM(v.totalComIva), 0) FROM Venda v WHERE v.utilizador.id = :userId AND v.dataVenda BETWEEN :inicio AND :fim")
    BigDecimal totalVendasPorPeriodo(Long userId, LocalDateTime inicio, LocalDateTime fim);
}