package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.ContaCorrenteExtratoDTO;
import pt.gestorflow.backend.dto.ContaCorrenteResumoDTO; // 🚀 O nosso novo tradutor
import pt.gestorflow.backend.dto.projection.ContaCorrenteExtratoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteFornecedorResumoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteResumoProjection;
import pt.gestorflow.backend.repository.ContaCorrenteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaCorrenteService {

    private final ContaCorrenteRepository repository;
    private final AuthService authService;

    // ==========================================
    // CLIENTES
    // ==========================================

    @Transactional(readOnly = true)
    public List<ContaCorrenteResumoDTO> obterResumoClientes() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // Vai buscar a interface de projeção bruta
        List<ContaCorrenteResumoProjection> projecoes = repository.obterResumoContasCorrentesClientes(utilizadorId);

        // Converte para o DTO que o Angular entende, tratando os nomes dos campos
        return projecoes.stream().map(p -> new ContaCorrenteResumoDTO(
                p.getClienteId(),
                null,
                p.getNomeCliente(), // Traduz 'nomeCliente' para 'nome'
                p.getTotalFaturado(),
                p.getTotalPago(),
                p.getSaldoPendente()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ContaCorrenteExtratoDTO> obterExtratoCliente(Long clienteId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        List<ContaCorrenteExtratoProjection> extratoBruto = repository.obterExtratoCliente(clienteId, utilizadorId);

        List<ContaCorrenteExtratoDTO> extratoProcessado = new ArrayList<>();
        BigDecimal saldoAtual = BigDecimal.ZERO;

        for (ContaCorrenteExtratoProjection linha : extratoBruto) {
            // Lógica de Cliente: Débito (Venda) aumenta dívida, Crédito (Pagamento) diminui.
            BigDecimal debito = linha.getDebito() != null ? linha.getDebito() : BigDecimal.ZERO;
            BigDecimal credito = linha.getCredito() != null ? linha.getCredito() : BigDecimal.ZERO;

            saldoAtual = saldoAtual.add(debito).subtract(credito);

            extratoProcessado.add(new ContaCorrenteExtratoDTO(
                    linha.getDataMovimento(),
                    linha.getTipoDocumento(),
                    linha.getDescricao(),
                    debito,
                    credito,
                    saldoAtual
            ));
        }
        return extratoProcessado;
    }

    // ==========================================
    // FORNECEDORES
    // ==========================================

    @Transactional(readOnly = true)
    public List<ContaCorrenteResumoDTO> obterResumoFornecedores() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // Vai buscar a interface de projeção bruta
        List<ContaCorrenteFornecedorResumoProjection> projecoes = repository.obterResumoContasCorrentesFornecedores(utilizadorId);

        // Mapeia os campos específicos do fornecedor para a estrutura genérica do Angular
        return projecoes.stream().map(p -> new ContaCorrenteResumoDTO(
                null,
                p.getFornecedorId(),
                p.getNomeFornecedor(), // Traduz 'nomeFornecedor' para 'nome'
                p.getTotalComprado(),  // Traduz 'totalComprado' para 'totalFaturado' (campo genérico)
                p.getTotalPago(),
                p.getSaldoPendente()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ContaCorrenteExtratoDTO> obterExtratoFornecedor(Long fornecedorId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        List<ContaCorrenteExtratoProjection> extratoBruto = repository.obterExtratoFornecedor(fornecedorId, utilizadorId);

        List<ContaCorrenteExtratoDTO> extratoProcessado = new ArrayList<>();
        BigDecimal saldoAtual = BigDecimal.ZERO;

        for (ContaCorrenteExtratoProjection linha : extratoBruto) {
            // Lógica de Fornecedor: Crédito (Compra) aumenta dívida, Débito (Pagamento feito por nós) diminui.
            BigDecimal debito = linha.getDebito() != null ? linha.getDebito() : BigDecimal.ZERO;
            BigDecimal credito = linha.getCredito() != null ? linha.getCredito() : BigDecimal.ZERO;

            saldoAtual = saldoAtual.add(credito).subtract(debito);

            extratoProcessado.add(new ContaCorrenteExtratoDTO(
                    linha.getDataMovimento(),
                    linha.getTipoDocumento(),
                    linha.getDescricao(),
                    debito,
                    credito,
                    saldoAtual
            ));
        }
        return extratoProcessado;
    }
}