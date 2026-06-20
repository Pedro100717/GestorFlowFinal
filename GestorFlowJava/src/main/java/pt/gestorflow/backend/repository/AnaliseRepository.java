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
            -- 🚀 VENDAS: Tabela no plural (linhas_venda) e coluna tx_iva_id
            SELECT 
                lv.centro_custo_id, 
                lv.seccao_homo_id, 
                (lv.quantidade * lv.preco_unitario) AS venda_base, 
                (lv.quantidade * lv.preco_unitario * (COALESCE(t.valor, 0) / 100.0)) AS venda_iva,
                0 AS compra_base, 
                0 AS compra_iva
            FROM linhas_venda lv
            JOIN vendas v ON lv.venda_id = v.id
            LEFT JOIN tx_iva t ON lv.tx_iva_id = t.id
            WHERE v.utilizador_id = :userId
            
            UNION ALL
            
            -- 🚀 COMPRAS: Tabela no plural (linhas_compra) e coluna tx_iva_id
            SELECT 
                lc.centro_custo_id, 
                lc.seccao_homo_id, 
                0 AS venda_base, 
                0 AS venda_iva,
                (lc.quantidade * lc.preco_unitario) AS compra_base,
                (lc.quantidade * lc.preco_unitario * (COALESCE(t.valor, 0) / 100.0)) AS compra_iva
            FROM linhas_compra lc
            JOIN compras c ON lc.compra_id = c.id
            LEFT JOIN tx_iva t ON lc.tx_iva_id = t.id
            WHERE c.utilizador_id = :userId
        ) uniao
        LEFT JOIN centro_custo cc ON uniao.centro_custo_id = cc.id
        LEFT JOIN seccao_homo sh ON uniao.seccao_homo_id = sh.id
        
        -- 🚀 O GROUP BY recolhe os dados limpos
        GROUP BY cc.codigo, cc.nome, sh.codigo, sh.nome
        ORDER BY cc.codigo, sh.codigo
    """)
    List<AnaliseAnaliticaProjection> obterAnaliseVendasCompras(@Param("userId") Long userId);
}