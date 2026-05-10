package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.dto.AnaliseAnaliticaProjection;
import pt.gestorflow.backend.model.Venda;
import java.util.List;

public interface AnaliseRepository extends JpaRepository<Venda, Long> {

    @Query(nativeQuery = true, value = """
        SELECT 
            COALESCE(cc.codigo, 'N/A') AS centroCustoCodigo,
            COALESCE(cc.nome, 'Sem Centro') AS centroCustoNome,
            
            COALESCE(sh.codigo, 'N/A') AS seccaoCodigo,
            COALESCE(sh.nome, 'Sem Secção') AS seccaoNome,
            
            -- 📈 VALORES OPERACIONAIS (SEM IVA)
            COALESCE(SUM(uniao.venda_base), 0) AS totalVendasSemIva,
            COALESCE(SUM(uniao.compra_base), 0) AS totalComprasSemIva,
            (COALESCE(SUM(uniao.venda_base), 0) - COALESCE(SUM(uniao.compra_base), 0)) AS margemBruta,
            
            -- ⚖️ VALORES FISCAIS (SÓ IVA)
            COALESCE(SUM(uniao.venda_iva), 0) AS totalIvaVendas,
            COALESCE(SUM(uniao.compra_iva), 0) AS totalIvaCompras,
            (COALESCE(SUM(uniao.venda_iva), 0) - COALESCE(SUM(uniao.compra_iva), 0)) AS saldoIva
            
        FROM (
            -- Vendas: Já temos os campos separados no modelo
            SELECT 
                centro_custo_id, seccao_homo_id, 
                total_sem_iva AS venda_base, 
                (total_com_iva - total_sem_iva) AS venda_iva,
                0 AS compra_base, 0 AS compra_iva
            FROM vendas WHERE utilizador_id = :userId
            
            UNION ALL
            
            -- Compras: Calculamos a base (qtd * preco) e o IVA (total - base)
            SELECT 
                centro_custo_id, seccao_homo_id, 
                0 AS venda_base, 0 AS venda_iva,
                (quantidade * preco_unitario) AS compra_base,
                (total - (quantidade * preco_unitario)) AS compra_iva
            FROM compras WHERE utilizador_id = :userId
        ) uniao
        LEFT JOIN centro_custo cc ON uniao.centro_custo_id = cc.id
        LEFT JOIN seccao_homo sh ON uniao.seccao_homo_id = sh.id
        -- 🚀 ADICIONADO: Os nomes têm de ir para o GROUP BY
        GROUP BY cc.codigo, cc.nome, sh.codigo, sh.nome
        ORDER BY cc.codigo, sh.codigo
    """)
    List<AnaliseAnaliticaProjection> obterAnaliseVendasCompras(@Param("userId") Long userId);
}