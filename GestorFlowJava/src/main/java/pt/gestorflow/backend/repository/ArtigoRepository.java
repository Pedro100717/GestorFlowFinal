package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.gestorflow.backend.model.Artigo;
import pt.gestorflow.backend.model.Mercadoria; // <--- FALTAVA ISTO PARA O EDITOR NÃO RECLAMAR

import java.math.BigDecimal;

public interface ArtigoRepository extends JpaRepository<Artigo, Long> {

    Page<Artigo> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // O uso explícito de 'value =' costuma calar os falsos positivos do VS Code
    @Query(value = "SELECT COALESCE(SUM(m.ultimoPrecoCusto * m.stockAtual), 0) FROM Mercadoria m WHERE m.utilizador.id = :userId")
    BigDecimal valorTotalStock(@Param("userId") Long userId);
}