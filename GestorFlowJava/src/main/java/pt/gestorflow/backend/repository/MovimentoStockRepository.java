package pt.gestorflow.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.MovimentoStock;

public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {

    // Para ver o histórico de um artigo específico
    Page<MovimentoStock> findAllByMercadoriaIdAndUtilizadorId(Long mercadoriaId, Long utilizadorId, Pageable pageable);

    // Para ver o histórico geral de acertos
    Page<MovimentoStock> findAllByUtilizadorId(Long utilizadorId, Pageable pageable);
}