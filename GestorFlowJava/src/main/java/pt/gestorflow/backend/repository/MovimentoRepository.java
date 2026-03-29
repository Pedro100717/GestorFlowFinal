package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.gestorflow.backend.model.Movimento;

import java.math.BigDecimal;
import java.util.List;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    List<Movimento> findAllByContaIdOrderByDataMovimentoDesc(Long contaId);

    // 1. Quanto pagámos a este Fornecedor a partir desta Conta específica?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.fornecedor.id = :fornecedorId AND m.conta.id = :contaId AND m.tipo = 'DEBITO' AND m.utilizador.id = :userId")
    BigDecimal totalPagoAFornecedorPorConta(@Param("fornecedorId") Long fornecedorId, @Param("contaId") Long contaId, @Param("userId") Long userId);

    // 2. Quanto pagámos a este Fornecedor no total?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.fornecedor.id = :fornecedorId AND m.tipo = 'DEBITO' AND m.utilizador.id = :userId")
    BigDecimal totalGastoComFornecedor(@Param("fornecedorId") Long fornecedorId, @Param("userId") Long userId);

    // 3. O Lucro / Fluxo Real da Conta (Agora em JPQL seguro e isolado por utilizador)
    @Query("SELECT COALESCE(SUM(CASE WHEN m.tipo = 'CREDITO' THEN m.valor ELSE (m.valor * -1) END), 0) FROM Movimento m WHERE m.conta.id = :contaId AND m.utilizador.id = :userId")
    BigDecimal lucroRealDaConta(@Param("contaId") Long contaId, @Param("userId") Long userId);

    // 4. Quanto recebemos deste Cliente no total?
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM Movimento m WHERE m.cliente.id = :clienteId AND m.tipo = 'CREDITO' AND m.utilizador.id = :userId")
    BigDecimal totalRecebidoDeCliente(@Param("clienteId") Long clienteId, @Param("userId") Long userId);
}