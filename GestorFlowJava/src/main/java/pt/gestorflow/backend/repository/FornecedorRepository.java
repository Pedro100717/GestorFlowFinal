package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Fornecedor;
import java.util.List;
import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    // Listar tudo (Já tinhas)
    List<Fornecedor> findAllByUtilizadorId(Long utilizadorId);

    // NOVO: Para Editar/Eliminar com segurança
    Optional<Fornecedor> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // NOVO: Para evitar NIFs duplicados
    boolean existsByNifAndUtilizadorId(String nif, Long utilizadorId);
}