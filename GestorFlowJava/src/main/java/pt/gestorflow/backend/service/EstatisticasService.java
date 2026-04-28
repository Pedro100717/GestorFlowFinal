package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstatisticasService {

    private final MovimentoRepository movimentoRepository;
    private final ContaBancariaRepository contaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ClienteRepository clienteRepository;
    private final AuthService authService; // 🚀 Injeção do Segurança Central

    public EstatisticaContaDTO getLucroDaConta(Long contaId) {
        // 🚀 Busca o ID de forma estrita
        Long userId = authService.getUtilizadorAutenticadoId();

        // 1. Busca a conta e garante que é do utilizador
        ContaBancaria conta = contaRepository.findByIdAndUtilizadorId(contaId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Conta não encontrada ou acesso negado."));

        // 2. Calcula a métrica
        BigDecimal lucro = movimentoRepository.lucroRealDaConta(contaId, userId);

        // 3. Monta o contexto completo para o Angular
        return EstatisticaContaDTO.builder()
                .contaId(conta.getId())
                .contaNome(conta.getNome())
                .lucroReal(lucro != null ? lucro : BigDecimal.ZERO)
                .moeda("EUR") // Em sistemas multi-moeda, isto viria da configuração da conta
                .build();
    }

    public EstatisticaFornecedorDTO getTotalGastoComFornecedor(Long fornecedorId) {
        Long userId = authService.getUtilizadorAutenticadoId();

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