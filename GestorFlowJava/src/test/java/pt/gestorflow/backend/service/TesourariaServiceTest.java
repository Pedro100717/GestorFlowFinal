package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.gestorflow.backend.dto.ConfirmarPagamentoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesourariaServiceTest {

    // 1. O Arsenal Completo: Todos os Repositórios que o TesourariaService usa no construtor
    @Mock private ContaBancariaRepository contaRepository;
    @Mock private MovimentoRepository movimentoRepository;
    @Mock private CompraRepository compraRepository;
    @Mock private VendaRepository vendaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private UtilizadorRepository utilizadorRepository;
    @Mock private MovimentoPlaneadoRepository movimentoPlaneadoRepository;
    @Mock private DocumentoTesourariaRepository documentoTesourariaRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private TesourariaService tesourariaService;

    private Utilizador userMock;
    private ContaBancaria contaMock;
    private Venda vendaMock;
    private Cliente clienteMock;

    @BeforeEach
    void setUp() {
        userMock = new Utilizador();
        userMock.setId(1L);

        contaMock = new ContaBancaria();
        contaMock.setId(10L);
        contaMock.setNome("Caixa Geral de Depósitos");
        contaMock.setSaldo(new BigDecimal("1000.00")); // Saldo inicial antes de pagarem

        clienteMock = new Cliente();
        clienteMock.setId(5L);
        clienteMock.setNome("Cliente Teste");

        vendaMock = new Venda();
        vendaMock.setId(100L);
        vendaMock.setTotalComIva(new BigDecimal("250.00")); // Fatura de 250€
        vendaMock.setValorPago(BigDecimal.ZERO);
        vendaMock.setEstadoPagamento(EstadoPagamento.PENDENTE);
        vendaMock.setCliente(clienteMock);
        vendaMock.setUtilizador(userMock);
    }

    @Test
    void confirmarTransacao_ComPagamentoTotalVenda_AtualizaSaldoEEstadoParaPago() {
        // ARRANGE
        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(userMock));

        // Simular que a Fatura e a Conta existem na BD
        when(vendaRepository.findByIdAndUtilizadorId(100L, 1L)).thenReturn(Optional.of(vendaMock));
        when(contaRepository.findByIdAndUtilizadorId(10L, 1L)).thenReturn(Optional.of(contaMock));

        ConfirmarPagamentoDTO dto = new ConfirmarPagamentoDTO();
        dto.setDocumentoId(100L);
        dto.setTipoDocumento("VENDA");
        dto.setContaBancariaId(10L);
        dto.setValorAPagar(new BigDecimal("250.00")); // Pagamento exato da totalidade (podes até testar com valores maiores que ele vai aceitar!)
        dto.setDataPagamento(LocalDate.now());

        // ACT
        tesourariaService.confirmarTransacao(dto);

        // ASSERT
        // 1. Estado mudou para pago?
        assertEquals(EstadoPagamento.PAGO, vendaMock.getEstadoPagamento(), "A fatura devia estar PAGA.");

        // 2. Valor Pago na fatura foi atualizado?
        assertEquals(0, new BigDecimal("250.00").compareTo(vendaMock.getValorPago()), "O valor pago não bate certo.");

        // 3. A Matemática funcionou? (1000€ iniciais + 250€ recebidos = 1250€)
        assertEquals(0, new BigDecimal("1250.00").compareTo(contaMock.getSaldo()), "O saldo da conta não somou a receita corretamente.");

        // 4. Garante que as atualizações foram mandadas para a base de dados
        verify(movimentoRepository, times(1)).save(any(Movimento.class));
        verify(contaRepository, times(1)).save(contaMock);
        verify(vendaRepository, times(1)).save(vendaMock);
    }
}