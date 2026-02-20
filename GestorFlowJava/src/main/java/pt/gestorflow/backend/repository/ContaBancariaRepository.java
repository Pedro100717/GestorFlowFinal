package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.gestorflow.backend.model.ContaBancaria;

import java.math.BigDecimal;
import java.util.List;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {
    List<ContaBancaria> findAllByUtilizadorId(Long utilizadorId);

    @Query("SELECT SUM(c.saldo) FROM ContaBancaria c WHERE c.utilizador.id = :userId")
    BigDecimal saldoTotal(Long userId);
}