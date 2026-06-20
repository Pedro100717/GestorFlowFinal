package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.LinhaVenda;

import java.util.List;

@Repository
public interface LinhaVendaRepository extends JpaRepository<LinhaVenda, Long> {

    // Método utilitário para o futuro (ex: Estatísticas de Venda por Artigo)
    List<LinhaVenda> findByArtigoId(Long artigoId);
}