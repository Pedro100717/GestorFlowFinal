package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.repository.MovimentoRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstatisticasService {

    private final MovimentoRepository movimentoRepository;

    // 1. Lucro real de uma conta específica
    public BigDecimal getLucroDaConta(Long contaId) {
        return movimentoRepository.lucroRealDaConta(contaId);
    }

    // 2. Total de dinheiro pago a um fornecedor
    public BigDecimal getTotalGastoComFornecedor(Long fornecedorId) {
        return movimentoRepository.totalGastoComFornecedor(fornecedorId);
    }

    // 3. Total de dinheiro recebido de um cliente
    public BigDecimal getTotalRecebidoDeCliente(Long clienteId) {
        return movimentoRepository.totalRecebidoDeCliente(clienteId);
    }
}