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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TesourariaServiceTest {

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
        contaMock.setSaldo(new BigDecimal("1000.00"));

        clienteMock = new Cliente();
        clienteMock.setId(5L);
        clienteMock.setNome("Cliente Teste");

        vendaMock = new Venda();
        vendaMock.setId(100L);
        vendaMock.setTotalComIva(new BigDecimal("250.00"));
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

        when(vendaRepository.findByIdAndUtilizadorId(100L, 1L)).thenReturn(Optional.of(vendaMock));
        when(contaRepository.findByIdAndUtilizadorId(10L, 1L)).thenReturn(Optional.of(contaMock));

        ConfirmarPagamentoDTO dto = new ConfirmarPagamentoDTO();
        dto.setDocumentoId(100L);
        dto.setTipoDocumento("VENDA");
        dto.setContaBancariaId(10L);
        dto.setValorAPagar(new BigDecimal("250.00"));
        dto.setDataPagamento(LocalDate.now());

        // ACT
        // 🚀 A chave de idempotência foi adicionada como segundo parâmetro para corresponder à assinatura do método
        tesourariaService.confirmarTransacao(dto, UUID.randomUUID().toString());

        // ASSERT
        assertEquals(EstadoPagamento.PAGO, vendaMock.getEstadoPagamento(), "A fatura devia estar PAGA.");
        assertEquals(0, new BigDecimal("250.00").compareTo(vendaMock.getValorPago()), "O valor pago não bate certo.");
        assertEquals(0, new BigDecimal("1250.00").compareTo(contaMock.getSaldo()), "O saldo da conta não somou a receita corretamente.");

        verify(movimentoRepository, times(1)).save(any(Movimento.class));
        verify(contaRepository, times(1)).save(contaMock);
        verify(vendaRepository, times(1)).save(vendaMock);
    }
}