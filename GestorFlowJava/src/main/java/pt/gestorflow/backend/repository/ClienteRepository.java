package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Cliente;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Page<Cliente> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    boolean existsByNifAndUtilizadorId(String nif, Long utilizadorId);

    Optional<Cliente> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // NOVO: Contar quantos clientes o utilizador tem
    long countByUtilizadorId(Long utilizadorId);
}