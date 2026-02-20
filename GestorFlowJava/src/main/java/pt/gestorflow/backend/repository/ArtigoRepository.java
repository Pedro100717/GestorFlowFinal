package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.Artigo;

import java.math.BigDecimal;

public interface ArtigoRepository extends JpaRepository<Artigo, Long> {

    Page<Artigo> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // CORREÇÃO:
    // 1. Mudámos "FROM Artigo a" para "FROM Mercadoria m"
    // 2. Removemos "AND a.movimentaStock = true" (porque ser Mercadoria já implica ter stock)
    // 3. Usamos "m.stockAtual" que agora existe na classe Mercadoria
    @Query(value = """
        SELECT COALESCE(SUM(ultimo_preco_custo * stock_atual), 0) 
        FROM artigos 
        WHERE utilizador_id = :userId 
        AND tipo_artigo = 'MERCADORIA'
    """, nativeQuery = true)
    BigDecimal valorTotalStock(Long userId);
}