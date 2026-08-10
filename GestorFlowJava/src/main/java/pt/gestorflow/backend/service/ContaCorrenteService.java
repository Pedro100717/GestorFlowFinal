package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.ContaCorrenteExtratoDTO;
import pt.gestorflow.backend.dto.ContaCorrenteResumoDTO;
import pt.gestorflow.backend.dto.projection.ContaCorrenteExtratoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteFornecedorResumoProjection;
import pt.gestorflow.backend.dto.projection.ContaCorrenteResumoProjection;
import pt.gestorflow.backend.repository.ContaCorrenteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j // 🚀 Anotação Mágica
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

        // 🛡️ DEBUG: Rotina normal, não polui o log de produção
        log.debug("Resumo geral de Contas Correntes (Clientes) solicitado pelo utilizador ID: {}", utilizadorId);

        List<ContaCorrenteResumoProjection> projecoes = repository.obterResumoContasCorrentesClientes(utilizadorId);

        return projecoes.stream().map(p -> new ContaCorrenteResumoDTO(
                p.getClienteId(),
                null,
                p.getNomeCliente(),
                p.getTotalFaturado(),
                p.getTotalPago(),
                p.getSaldoPendente()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ContaCorrenteExtratoDTO> obterExtratoCliente(Long clienteId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ INFO: Registo de Auditoria. Sabemos exatamente quem foi cuscar a dívida de quem.
        log.info("Auditoria Financeira: Extrato detalhado gerado para o Cliente ID: {} (Utilizador ID: {})", clienteId, utilizadorId);

        List<ContaCorrenteExtratoProjection> extratoBruto = repository.obterExtratoCliente(clienteId, utilizadorId);

        List<ContaCorrenteExtratoDTO> extratoProcessado = new ArrayList<>();
        BigDecimal saldoAtual = BigDecimal.ZERO;

        for (ContaCorrenteExtratoProjection linha : extratoBruto) {
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

        // 🛡️ DEBUG: Rotina normal
        log.debug("Resumo geral de Contas Correntes (Fornecedores) solicitado pelo utilizador ID: {}", utilizadorId);

        List<ContaCorrenteFornecedorResumoProjection> projecoes = repository.obterResumoContasCorrentesFornecedores(utilizadorId);

        return projecoes.stream().map(p -> new ContaCorrenteResumoDTO(
                null,
                p.getFornecedorId(),
                p.getNomeFornecedor(),
                p.getTotalComprado(),
                p.getTotalPago(),
                p.getSaldoPendente()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ContaCorrenteExtratoDTO> obterExtratoFornecedor(Long fornecedorId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ INFO: Registo de Auditoria Financeira.
        log.info("Auditoria Financeira: Extrato detalhado gerado para o Fornecedor ID: {} (Utilizador ID: {})", fornecedorId, utilizadorId);

        List<ContaCorrenteExtratoProjection> extratoBruto = repository.obterExtratoFornecedor(fornecedorId, utilizadorId);

        List<ContaCorrenteExtratoDTO> extratoProcessado = new ArrayList<>();
        BigDecimal saldoAtual = BigDecimal.ZERO;

        for (ContaCorrenteExtratoProjection linha : extratoBruto) {
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