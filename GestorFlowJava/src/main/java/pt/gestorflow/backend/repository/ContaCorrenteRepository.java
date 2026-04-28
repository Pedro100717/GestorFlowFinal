package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.dto.projection.ContaCorrenteExtratoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteFornecedorResumoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteResumoProjection;
import pt.gestorflow.backend.model.Cliente;

import java.util.List;

@Repository
public interface ContaCorrenteRepository extends JpaRepository<Cliente, Long> {

    // Radar Macro: Resumo de todos os Clientes
    @Query(value = """
        SELECT 
            c.id AS clienteId,
            c.nome AS nomeCliente,
            COALESCE(v.total_faturado, 0) AS totalFaturado,
            COALESCE(t.total_pago, 0) AS totalPago,
            (COALESCE(v.total_faturado, 0) - COALESCE(t.total_pago, 0)) AS saldoPendente
        FROM clientes c
        LEFT JOIN (
            SELECT cliente_id, SUM(total_com_iva) AS total_faturado
            FROM vendas
            WHERE utilizador_id = :utilizadorId
            GROUP BY cliente_id
        ) v ON c.id = v.cliente_id
        LEFT JOIN (
            SELECT cliente_id, SUM(valor) AS total_pago
            FROM movimentos_tesouraria
            WHERE utilizador_id = :utilizadorId AND tipo = 'ENTRADA'
            GROUP BY cliente_id
        ) t ON c.id = t.cliente_id
        WHERE c.utilizador_id = :utilizadorId
        ORDER BY saldoPendente DESC
        """, nativeQuery = true)
    List<ContaCorrenteResumoProjection> obterResumoContasCorrentesClientes(@Param("utilizadorId") Long utilizadorId);

    // Visão Micro: Extrato Cronológico de um Cliente Específico
    @Query(value = """
        SELECT 
            v.data_venda AS dataMovimento,
            'FATURA' AS tipoDocumento,
            CONCAT('Fatura #', v.id) AS descricao,
            v.total_com_iva AS debito,
            0 AS credito
        FROM vendas v
        WHERE v.cliente_id = :clienteId AND v.utilizador_id = :utilizadorId
        
        UNION ALL
        
        SELECT 
            m.data_movimento AS dataMovimento,
            'RECEBIMENTO' AS tipoDocumento,
            m.descricao AS descricao,
            0 AS debito,
            m.valor AS credito
        FROM movimentos_tesouraria m
        WHERE m.cliente_id = :clienteId AND m.utilizador_id = :utilizadorId AND m.tipo = 'ENTRADA'
        
        ORDER BY dataMovimento ASC
        """, nativeQuery = true)
    List<ContaCorrenteExtratoProjection> obterExtratoCliente(@Param("clienteId") Long clienteId, @Param("utilizadorId") Long utilizadorId);

    // --------------------------------------------------------
    // FORNECEDORES
    // --------------------------------------------------------

    // Radar Macro: Resumo de todos os Fornecedores
    @Query(value = """
        SELECT 
            f.id AS fornecedorId,
            f.nome AS nomeFornecedor,
            COALESCE(c.total_comprado, 0) AS totalComprado,
            COALESCE(t.total_pago, 0) AS totalPago,
            (COALESCE(c.total_comprado, 0) - COALESCE(t.total_pago, 0)) AS saldoPendente
        FROM fornecedores f
        LEFT JOIN (
            SELECT fornecedor_id, SUM(total) AS total_comprado
            FROM compras
            WHERE utilizador_id = :utilizadorId
            GROUP BY fornecedor_id
        ) c ON f.id = c.fornecedor_id
        LEFT JOIN (
            SELECT fornecedor_id, SUM(valor) AS total_pago
            FROM movimentos_tesouraria
            WHERE utilizador_id = :utilizadorId AND tipo = 'SAIDA'
            GROUP BY fornecedor_id
        ) t ON f.id = t.fornecedor_id
        WHERE f.utilizador_id = :utilizadorId
        ORDER BY saldoPendente DESC
        """, nativeQuery = true)
    List<ContaCorrenteFornecedorResumoProjection> obterResumoContasCorrentesFornecedores(@Param("utilizadorId") Long utilizadorId);

    // Visão Micro: Extrato Cronológico de um Fornecedor Específico
    @Query(value = """
        SELECT 
            c.data_compra AS dataMovimento,
            'FATURA_COMPRA' AS tipoDocumento,
            CONCAT('Fatura #', COALESCE(c.numero_fatura_fornecedor, CAST(c.id AS VARCHAR))) AS descricao,
            0 AS debito,
            c.total AS credito
        FROM compras c
        WHERE c.fornecedor_id = :fornecedorId AND c.utilizador_id = :utilizadorId
        
        UNION ALL
        
        SELECT 
            m.data_movimento AS dataMovimento,
            'PAGAMENTO' AS tipoDocumento,
            m.descricao AS descricao,
            m.valor AS debito,
            0 AS credito
        FROM movimentos_tesouraria m
        WHERE m.fornecedor_id = :fornecedorId AND m.utilizador_id = :utilizadorId AND m.tipo = 'SAIDA'
        
        ORDER BY dataMovimento ASC
        """, nativeQuery = true)
    List<ContaCorrenteExtratoProjection> obterExtratoFornecedor(@Param("fornecedorId") Long fornecedorId, @Param("utilizadorId") Long utilizadorId);
}