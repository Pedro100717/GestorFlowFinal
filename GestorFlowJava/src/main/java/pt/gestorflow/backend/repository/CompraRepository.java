package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.Compra;
import pt.gestorflow.backend.model.EstadoPagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Usamos o @EntityGraph para que o "buscarPorId" seja uma autêntica bala de performance.
    @EntityGraph(attributePaths = {"fornecedor", "artigo", "contaBancaria", "centroCusto", "seccaoHomo", "taxaIva"})
    Optional<Compra> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    //Vai buscar tudo numa so Query
    @EntityGraph(attributePaths = {"fornecedor", "artigo", "contaBancaria", "centroCusto", "seccaoHomo", "taxaIva"})
    Page<Compra> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    //Soma total de compras da vida inteira
    @Query("SELECT COALESCE(SUM(c.total), 0) FROM Compra c WHERE c.utilizador.id = :userId")
    BigDecimal totalGastos(Long userId);

    // 🚀 A MUDANÇA INDUSTRIAL: 'In' no nome do método e 'List<EstadoPagamento>' nos parâmetros
    List<Compra> findAllByUtilizadorIdAndEstadoPagamentoIn(Long utilizadorId, List<EstadoPagamento> estados);
}