package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.gestorflow.backend.dto.OrcamentoDTO;
import pt.gestorflow.backend.dto.OrcamentoResponseDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock private OrcamentoRepository orcamentoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ArtigoRepository artigoRepository;
    @Mock private TxIvaRepository txIvaRepository;
    @Mock private VendaService vendaService;
    @Mock private UtilizadorRepository utilizadorRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private OrcamentoService orcamentoService;

    private Utilizador utilizadorMock;
    private Cliente clienteMock;
    private Mercadoria artigoMock;
    private TxIva ivaMock;

    @BeforeEach
    void setUp() {
        utilizadorMock = new Utilizador();
        utilizadorMock.setId(1L);

        clienteMock = new Cliente();
        clienteMock.setId(10L);
        clienteMock.setNome("Cliente VIP");

        artigoMock = new Mercadoria();
        artigoMock.setId(50L);
        artigoMock.setNome("Monitor 4K");
        artigoMock.setUltimoPrecoCusto(new BigDecimal("100.00")); // Custa 100€ à empresa

        ivaMock = new TxIva();
        ivaMock.setId(1L);
        ivaMock.setValor(new BigDecimal("23.00")); // IVA a 23%
    }

    @Test
    void criarOrcamento_ComSucesso_CalculaTotaisEMargemDeLucro() {
        // ARRANGE
        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(utilizadorMock));
        when(clienteRepository.findByIdAndUtilizadorId(10L, 1L)).thenReturn(Optional.of(clienteMock));

        // 🚀 Mock das procuras em lote (Otimização N+1)
        when(artigoRepository.findAllByIdInAndUtilizadorId(List.of(50L), 1L)).thenReturn(List.of(artigoMock));
        when(txIvaRepository.findAllById(List.of(1L))).thenReturn(List.of(ivaMock));

        // Devolver a entidade simulada quando o repository guardar
        when(orcamentoRepository.save(any(Orcamento.class))).thenAnswer(i -> i.getArgument(0));

        // DTO usando a classe correta: LinhaOrcamentoDTO
        OrcamentoDTO.LinhaOrcamentoDTO linha = new OrcamentoDTO.LinhaOrcamentoDTO();
        linha.setArtigoId(50L);
        linha.setTaxaIvaId(1L);
        linha.setQuantidade(new BigDecimal("2"));
        linha.setPrecoVendaUnitarioOverride(new BigDecimal("150.00")); // Vendido a 150€

        OrcamentoDTO dto = new OrcamentoDTO();
        dto.setClienteId(10L);
        dto.setLinhas(List.of(linha));

        // ACT
        OrcamentoResponseDTO resposta = orcamentoService.criarOrcamento(dto);

        // ASSERT
        assertNotNull(resposta);

        // Totais Gerais: 2 * 150€ = 300€ Base + 23% = 369€
        assertEquals(0, new BigDecimal("300.00").compareTo(resposta.getTotalSemIva()), "Total Sem IVA falhou");
        assertEquals(0, new BigDecimal("369.00").compareTo(resposta.getTotalComIva()), "Total Com IVA falhou");

        // Verificação Crítica: A matemática da rentabilidade (Markup)
        OrcamentoResponseDTO.LinhaResponseDTO linhaResposta = resposta.getLinhas().get(0);

        assertEquals(0, new BigDecimal("100.00").compareTo(linhaResposta.getPrecoCustoUnitario()));
        assertEquals(0, new BigDecimal("150.00").compareTo(linhaResposta.getPrecoVendaUnitario()));

        // A TUA LÓGICA: ((150 - 100) / 100) * 100 = 50% de Margem!
        assertEquals(0, new BigDecimal("50.0000").compareTo(linhaResposta.getMargemLucroPercentual()), "A margem/markup falhou");

        verify(orcamentoRepository, times(1)).save(any(Orcamento.class));
    }
}