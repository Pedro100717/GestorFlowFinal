package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO;
import pt.gestorflow.backend.dto.LinhaCompraDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    // Repositórios necessários para o CompraService
    @Mock private CompraRepository compraRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private ArtigoRepository artigoRepository;
    @Mock private CentroCustoRepository centroCustoRepository;
    @Mock private SeccaoHomoRepository seccaoHomoRepository;
    @Mock private TxIvaRepository txIvaRepository;
    @Mock private MovimentoStockRepository movimentoStockRepository;
    @Mock private MovimentoPlaneadoRepository movimentoPlaneadoRepository;
    @Mock private UtilizadorRepository utilizadorRepository;

    @Mock private ArtigoService artigoService;
    @Mock private AuthService authService;

    @InjectMocks
    private CompraService compraService;

    private Utilizador userMock;
    private Fornecedor fornecedorMock;
    private Mercadoria artigoMock;
    private TxIva ivaMock;

    @BeforeEach
    void setUp() {
        userMock = new Utilizador();
        userMock.setId(1L);

        fornecedorMock = new Fornecedor();
        fornecedorMock.setId(10L);

        artigoMock = new Mercadoria();
        artigoMock.setId(50L);
        artigoMock.setNome("Rato de Computador");
        artigoMock.setStockAtual(BigDecimal.ZERO);

        ivaMock = new TxIva();
        ivaMock.setId(1L);
        ivaMock.setValor(new BigDecimal("23.00"));
    }

    @Test
    void registarCompra_ComSucesso_AtualizaStockEMatematica() {
        // ARRANGE
        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(utilizadorRepository.findById(1L)).thenReturn(Optional.of(userMock));
        when(fornecedorRepository.findByIdAndUtilizadorId(10L, 1L)).thenReturn(Optional.of(fornecedorMock));
        when(artigoRepository.findByIdAndUtilizadorId(50L, 1L)).thenReturn(Optional.of(artigoMock));
        when(txIvaRepository.findById(1L)).thenReturn(Optional.of(ivaMock));
        when(compraRepository.save(any(Compra.class))).thenAnswer(i -> i.getArgument(0));

        LinhaCompraDTO linha = new LinhaCompraDTO();
        linha.setArtigoId(50L);
        linha.setTaxaIvaId(1L);
        linha.setQuantidade(new BigDecimal("10"));
        linha.setPrecoUnitario(new BigDecimal("10.00"));

        CompraDTO dto = new CompraDTO();
        dto.setFornecedorId(10L);
        dto.setLinhas(List.of(linha));

        // ACT
        CompraResponseDTO resposta = compraService.registarCompra(dto);

        // ASSERT
        assertNotNull(resposta);
        // (10 * 10) + 23% = 123.00
        assertEquals(0, new BigDecimal("123.00").compareTo(resposta.getTotal()));

        // Verifica se o stock foi mexido no repositório
        verify(movimentoStockRepository, times(1)).save(any(MovimentoStock.class));
        verify(artigoRepository, atLeastOnce()).save(any(Artigo.class));
    }
}