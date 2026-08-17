package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.Fornecedor;

import java.util.Optional;

@Repository
public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    // 🚀 ATUALIZADO: Agora suporta paginação! Recebe um Pageable e devolve uma Page.
    Page<Fornecedor> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);

    // Para Editar/Eliminar com segurança (Garante que o fornecedor te pertence)
    Optional<Fornecedor> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // Para evitar NIFs duplicados na tua conta
    boolean existsByNifAndUtilizadorId(String nif, Long utilizadorId);
}