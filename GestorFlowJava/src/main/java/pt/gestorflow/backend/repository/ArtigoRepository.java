package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.gestorflow.backend.model.Artigo;
import pt.gestorflow.backend.model.Mercadoria;

import java.math.BigDecimal;
import java.util.List;     // <--- NOVO IMPORT ADICIONADO
import java.util.Optional;

public interface ArtigoRepository extends JpaRepository<Artigo, Long> {

    Page<Artigo> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // 🛡️ A TRANCA DE SEGURANÇA
    Optional<Artigo> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // 🚀 OTIMIZAÇÃO: Busca em Lote (Bulk Fetching) para evitar o N+1 Queries
    List<Artigo> findAllByIdInAndUtilizadorId(List<Long> ids, Long utilizadorId);

    @Query(value = "SELECT COALESCE(SUM(m.ultimoPrecoCusto * m.stockAtual), 0) FROM Mercadoria m WHERE m.utilizador.id = :userId")
    BigDecimal valorTotalStock(@Param("userId") Long userId);
}