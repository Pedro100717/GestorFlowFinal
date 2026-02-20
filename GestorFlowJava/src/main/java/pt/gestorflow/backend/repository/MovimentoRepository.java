package pt.gestorflow.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.gestorflow.backend.model.Movimento;
import java.util.List;

public interface MovimentoRepository extends JpaRepository<Movimento, Long> {
    // Buscar movimentos de uma conta ordenados por data (Extrato)
    List<Movimento> findAllByContaIdOrderByDataMovimentoDesc(Long contaId);
}