package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaClienteDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaContaDTO;
import pt.gestorflow.backend.dto.estatisticas.EstatisticaFornecedorDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.model.ContaBancaria;
import pt.gestorflow.backend.model.Fornecedor;
import pt.gestorflow.backend.repository.ClienteRepository;
import pt.gestorflow.backend.repository.ContaBancariaRepository;
import pt.gestorflow.backend.repository.FornecedorRepository;
import pt.gestorflow.backend.repository.MovimentoRepository;

import java.math.BigDecimal;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstatisticasService {

    private final MovimentoRepository movimentoRepository;
    private final ContaBancariaRepository contaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ClienteRepository clienteRepository;
    private final AuthService authService;

    public EstatisticaContaDTO getLucroDaConta(Long contaId) {
        Long userId = authService.getUtilizadorAutenticadoId();

        log.debug("A calcular lucro real da Conta ID: {} (Utilizador: {})", contaId, userId);

        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(contaId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada ou acesso negado."));

        BigDecimal lucro = movimentoRepository.lucroRealDaConta(contaId, userId);

        return EstatisticaContaDTO.builder()
                .contaId(conta.getId())
                .contaNome(conta.getNome())
                .lucroReal(lucro != null ? lucro : BigDecimal.ZERO)
                .moeda("EUR")
                .build();
    }

    public EstatisticaFornecedorDTO getTotalGastoComFornecedor(Long fornecedorId) {
        Long userId = authService.getUtilizadorAutenticadoId();

        log.debug("A calcular total gasto com Fornecedor ID: {} (Utilizador: {})", fornecedorId, userId);

        Fornecedor fornecedor = fornecedorRepository.findByIdAndUtilizadorId(fornecedorId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Fornecedor não encontrado ou acesso negado."));

        BigDecimal total = movimentoRepository.totalGastoComFornecedor(fornecedorId, userId);

        return EstatisticaFornecedorDTO.builder()
                .fornecedorId(fornecedor.getId())
                .fornecedorNome(fornecedor.getNome())
                .totalGasto(total != null ? total : BigDecimal.ZERO)
                .moeda("EUR")
                .build();
    }

    public EstatisticaClienteDTO getTotalRecebidoDeCliente(Long clienteId) {
        Long userId = authService.getUtilizadorAutenticadoId();

        log.debug("A calcular total recebido do Cliente ID: {} (Utilizador: {})", clienteId, userId);

        Cliente cliente = clienteRepository.findByIdAndUtilizadorId(clienteId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado ou acesso negado."));

        BigDecimal total = movimentoRepository.totalRecebidoDeCliente(clienteId, userId);

        return EstatisticaClienteDTO.builder()
                .clienteId(cliente.getId())
                .clienteNome(cliente.getNome())
                .totalRecebido(total != null ? total : BigDecimal.ZERO)
                .moeda("EUR")
                .build();
    }
}