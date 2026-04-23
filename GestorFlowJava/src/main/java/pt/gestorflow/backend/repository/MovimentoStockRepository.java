package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.MovimentoStock;

import java.util.Optional;

@Repository
public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {

    // 🛡️ O teu método original com a otimização
    @EntityGraph(attributePaths = {"mercadoria"})
    Optional<MovimentoStock> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // 🛡️ PARA O ECRÃ GERAL: Esconde as Compras e Vendas automáticas
    @EntityGraph(attributePaths = {"mercadoria"})
    @Query("SELECT m FROM MovimentoStock m WHERE m.utilizador.id = :utilizadorId " +
            "AND m.motivo NOT LIKE 'Compra a Fornecedor:%' " +
            "AND m.motivo NOT LIKE 'Venda a Cliente:%' ORDER BY m.dataMovimento DESC")
    Page<MovimentoStock> buscarApenasAcertosManuais(@Param("utilizadorId") Long utilizadorId, Pageable pageable);

    // 🛡️ PARA O MODAL: Usamos o método derivado do Spring (mais robusto para herança)
    @EntityGraph(attributePaths = {"mercadoria"})
    Page<MovimentoStock> findAllByMercadoriaIdAndUtilizadorId(Long mercadoriaId, Long utilizadorId, Pageable pageable);
}