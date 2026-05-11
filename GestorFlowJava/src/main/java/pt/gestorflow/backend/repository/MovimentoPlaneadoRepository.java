package pt.gestorflow.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.gestorflow.backend.model.MovimentoPlaneado;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovimentoPlaneadoRepository extends JpaRepository<MovimentoPlaneado, Long> {

    // 🚀 Usado pelo motor gráfico para projetar o futuro
    List<MovimentoPlaneado> findAllByUtilizadorIdAndAtivoTrue(Long utilizadorId);

    // 🚀 Usado pelo CRUD para listar todos (ativos ou não) na tabela de planeamento
    List<MovimentoPlaneado> findAllByUtilizadorId(Long utilizadorId);

    // 🚀 A SEGURANÇA MÁXIMA: Garante que um utilizador só acede aos seus próprios planos
    Optional<MovimentoPlaneado> findByIdAndUtilizadorId(Long id, Long utilizadorId);
}