package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.dto.AnaliseAnaliticaProjection;
import pt.gestorflow.backend.model.Venda;
import java.util.List;

public interface AnaliseRepository extends JpaRepository<Venda, Long> {

    // 🚀 OTIMIZAÇÃO INDUSTRIAL: Processa tudo na Base de Dados e devolve apenas o Resumo
    @Query(nativeQuery = true, value = """
        SELECT 
            COALESCE(cc.codigo, 'Sem Centro') AS centroCusto,
            COALESCE(sh.codigo, 'Sem Secção') AS seccaoHomo,
            COALESCE(SUM(uniao.vendas), 0) AS totalVendas,
            COALESCE(SUM(uniao.compras), 0) AS totalCompras,
            (COALESCE(SUM(uniao.vendas), 0) - COALESCE(SUM(uniao.compras), 0)) AS margem
        FROM (
            SELECT centro_custo_id, seccao_homo_id, total_com_iva AS vendas, 0 AS compras 
            FROM vendas WHERE utilizador_id = :userId
            UNION ALL
            SELECT centro_custo_id, seccao_homo_id, 0 AS vendas, total AS compras 
            FROM compras WHERE utilizador_id = :userId
        ) uniao
        LEFT JOIN centro_custo cc ON uniao.centro_custo_id = cc.id
        LEFT JOIN seccao_homo sh ON uniao.seccao_homo_id = sh.id
        GROUP BY cc.codigo, sh.codigo
        ORDER BY cc.codigo, sh.codigo
    """)
    List<AnaliseAnaliticaProjection> obterAnaliseVendasCompras(@Param("userId") Long userId);
}