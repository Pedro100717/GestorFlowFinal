package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.CentroCusto;
import java.util.List;
import java.util.Optional;

public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {

    // Para listar nas "comboboxes" do frontend
    List<CentroCusto> findAllByUtilizadorId(Long utilizadorId);

    // 🛡️ A TRANCA DE SEGURANÇA OBRIGATÓRIA PARA O SERVICE
    Optional<CentroCusto> findByIdAndUtilizadorId(Long id, Long utilizadorId);

    // 🚀 OTIMIZAÇÃO: Busca em Lote (Bulk Fetching) para evitar N+1 Queries
    List<CentroCusto> findAllByIdInAndUtilizadorId(List<Long> ids, Long utilizadorId);
}