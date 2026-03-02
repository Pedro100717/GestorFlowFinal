package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.model.Movimento;

import java.math.BigDecimal;
import java.util.List;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    // Buscar movimentos de uma conta ordenados por data (Extrato normal)
    List<Movimento> findAllByContaIdOrderByDataMovimentoDesc(Long contaId);

    // ==========================================
    // QUERIES DE ESTATÍSTICA E LUCRO (TESOURARIA)
    // ==========================================

    // 1. Quanto pagámos a este Fornecedor a partir desta Conta específica?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.fornecedor.id = :fornecedorId AND m.conta.id = :contaId AND m.tipo = 'DEBITO'")
    BigDecimal totalPagoAFornecedorPorConta(@Param("fornecedorId") Long fornecedorId, @Param("contaId") Long contaId);

    // 2. Quanto pagámos a este Fornecedor no total (juntando todas as nossas contas)?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.fornecedor.id = :fornecedorId AND m.tipo = 'DEBITO'")
    BigDecimal totalGastoComFornecedor(@Param("fornecedorId") Long fornecedorId);

    // 3. O Lucro / Fluxo Real da Conta (Soma dos Créditos menos a Soma dos Débitos)
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN tipo = 'CREDITO' THEN valor ELSE (valor * -1) END), 0) FROM movimentos_tesouraria WHERE conta_bancaria_id = :contaId", nativeQuery = true)
    BigDecimal lucroRealDaConta(@Param("contaId") Long contaId);

    // ==========================================
    // ESTATÍSTICAS DE CLIENTES
    // ==========================================

    // 4. Quanto recebemos deste Cliente no total (juntando todas as contas)?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.cliente.id = :clienteId AND m.tipo = 'CREDITO'")
    BigDecimal totalRecebidoDeCliente(@Param("clienteId") Long clienteId);

    // 5. Opcional: Quanto recebemos deste Cliente para uma conta específica?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.cliente.id = :clienteId AND m.conta.id = :contaId AND m.tipo = 'CREDITO'")
    BigDecimal totalRecebidoDeClientePorConta(@Param("clienteId") Long clienteId, @Param("contaId") Long contaId);
}