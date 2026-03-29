package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.MovimentoRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstatisticasService {

    private final MovimentoRepository movimentoRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public BigDecimal getLucroDaConta(Long contaId) {
        return movimentoRepository.lucroRealDaConta(contaId, getUtilizadorLogado().getId());
    }

    public BigDecimal getTotalGastoComFornecedor(Long fornecedorId) {
        return movimentoRepository.totalGastoComFornecedor(fornecedorId, getUtilizadorLogado().getId());
    }

    public BigDecimal getTotalRecebidoDeCliente(Long clienteId) {
        return movimentoRepository.totalRecebidoDeCliente(clienteId, getUtilizadorLogado().getId());
    }
}