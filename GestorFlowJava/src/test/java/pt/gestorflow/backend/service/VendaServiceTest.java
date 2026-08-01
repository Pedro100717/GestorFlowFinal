package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.gestorflow.backend.dto.LinhaVendaDTO;
import pt.gestorflow.backend.dto.VendaDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    // 1. TODAS as dependências do construtor rigorosamente declaradas
    @Mock private VendaRepository vendaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ArtigoRepository artigoRepository;
    @Mock private TxIvaRepository txIvaRepository;
    @Mock private MovimentoStockRepository movimentoStockRepository;
    @Mock private CentroCustoRepository centroCustoRepository;
    @Mock private SeccaoHomoRepository seccaoHomoRepository;
    @Mock private MovimentoPlaneadoRepository movimentoPlaneadoRepository;
    @Mock private UtilizadorRepository utilizadorRepository;

    @Mock private AuthService authService;
    @Mock private ArtigoService artigoService;

    @InjectMocks
    private VendaService vendaService;

    private Utilizador utilizadorMock;
    private Cliente clienteMock;
    private Mercadoria artigoMock;
    private TxIva ivaMock;
    private Venda vendaExistente;

    @BeforeEach
    void setUp() {
        utilizadorMock = new Utilizador();
        utilizadorMock.setId(1L);

        clienteMock = new Cliente();
        clienteMock.setId(10L);
        clienteMock.setNome("Cliente XPTO");

        artigoMock = new Mercadoria();
        artigoMock.setId(50L);
        artigoMock.setNome("Teclado Mecânico");
        artigoMock.setStockAtual(new BigDecimal("20.00"));

        ivaMock = new TxIva();
        ivaMock.setId(1L);
        ivaMock.setValor(new BigDecimal("23.00"));

        vendaExistente = new Venda();
        vendaExistente.setId(100L);
        vendaExistente.setEstadoPagamento(EstadoPagamento.PAGO); // Forçar estado para testar anulação
    }

    @Test
    void registarVenda_ComSucesso_CalculaIvaERemoveStock() {
        // ARRANGE
        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(utilizadorMock));
        when(clienteRepository.findByIdAndUtilizadorId(10L, 1L)).thenReturn(Optional.of(clienteMock));

        // 🚀 OS NOVOS MOCKS: Agora respondem a pedidos em Lote (Listas)
        when(artigoRepository.findAllByIdInAndUtilizadorId(List.of(50L), 1L)).thenReturn(List.of(artigoMock));
        when(txIvaRepository.findAllById(List.of(1L))).thenReturn(List.of(ivaMock));

        // Simular a gravação e devolver a própria entidade
        when(vendaRepository.save(any(Venda.class))).thenAnswer(i -> i.getArgument(0));

        LinhaVendaDTO linha = new LinhaVendaDTO();
        linha.setArtigoId(50L);
        linha.setTaxaIvaId(1L);
        linha.setQuantidade(new BigDecimal("2")); // 2 teclados
        linha.setPrecoUnitario(new BigDecimal("50.00")); // 50€ cada

        VendaDTO dto = new VendaDTO();
        dto.setClienteId(10L);
        dto.setLinhas(List.of(linha));

        // ACT
        VendaResponseDTO resposta = vendaService.registarVenda(dto);

        // ASSERT
        assertNotNull(resposta);

        // Matemática Base: 2 * 50€ = 100€
        assertEquals(0, new BigDecimal("100.00").compareTo(resposta.getTotalSemIva()), "O Total sem IVA falhou.");

        // Matemática com IVA: 100€ * 1.23 = 123€
        assertEquals(0, new BigDecimal("123.00").compareTo(resposta.getTotalComIva()), "O cálculo do IVA está incorreto.");

        // Verificações de Integração: Confirmar se o stock foi abatido
        verify(artigoService, times(1)).removerStock(eq(50L), eq(new BigDecimal("2")));
        verify(movimentoStockRepository, times(1)).save(any(MovimentoStock.class));
    }

    @Test
    void anularVenda_QueJaEstaPaga_LancaExcecaoSeguranca() {
        // ARRANGE
        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(utilizadorMock));
        when(vendaRepository.findByIdAndUtilizadorId(100L, 1L)).thenReturn(Optional.of(vendaExistente));

        // ACT & ASSERT
        IllegalStateException excecao = assertThrows(IllegalStateException.class, () -> {
            vendaService.anularVenda(100L);
        });

        assertEquals("Não é possível anular uma venda que já foi recebida na tesouraria (parcial ou totalmente).", excecao.getMessage());

        // Garantir que a base de dados não foi tocada após a interceção do erro
        verify(vendaRepository, never()).delete(any());
        verify(artigoService, never()).adicionarStock(any(), any());
    }
}