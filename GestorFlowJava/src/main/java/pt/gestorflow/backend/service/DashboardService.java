package pt.gestorflow.backend.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import pt.gestorflow.backend.dto.DashboardDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.Venda;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final VendaRepository vendaRepository;
    private final CompraRepository compraRepository;
    private final ArtigoRepository artigoRepository;
    private final ClienteRepository clienteRepository;
    private final AuthService authService;

    public DashboardDTO getResumo(LocalDate inicio, LocalDate fim) {
        Long userId = authService.getUtilizadorAutenticadoId();

        // 🚀 O Tratamento de Fallback: Se não houver filtro, varremos todo o histórico possível.
        LocalDate dataInicioSegura = (inicio != null) ? inicio : LocalDate.of(2000, 1, 1);
        LocalDate dataFimSegura = (fim != null) ? fim : LocalDate.of(2100, 12, 31);

        // Passamos as datas seguras aos Repositórios com IVA
        BigDecimal totalCompras = compraRepository.totalGastos(userId, dataInicioSegura, dataFimSegura);
        BigDecimal totalVendas = vendaRepository.totalVendasReais(userId, dataInicioSegura, dataFimSegura);

        //Obter os totais brutos
        BigDecimal vendasBase = vendaRepository.totalVendasBase(userId, dataInicioSegura, dataFimSegura);
        BigDecimal comprasBase = compraRepository.totalComprasBase(userId, dataInicioSegura, dataFimSegura);

        //calcular margem
        BigDecimal margemBruta = vendasBase.subtract(comprasBase);

        BigDecimal valorStock = artigoRepository.valorTotalStock(userId);

        List<Venda> ultimasVendas = vendaRepository.findRecentVendas(userId, dataInicioSegura, dataFimSegura, PageRequest.of(0, 5));

        List<VendaResponseDTO> ultimasVendasDTO = ultimasVendas.stream()
                .map(this::converterVendaParaDTO)
                .toList();

        return DashboardDTO.builder()
                .totalVendas(totalVendas != null ? totalVendas : BigDecimal.ZERO)
                .totalCompras(totalCompras != null ? totalCompras : BigDecimal.ZERO)
                .valorStock(valorStock != null ? valorStock : BigDecimal.ZERO)
                .margemBruta(margemBruta != null ? margemBruta : BigDecimal.ZERO)
                .ultimasVendas(ultimasVendasDTO)
                .build();
    }

    private VendaResponseDTO converterVendaParaDTO(Venda venda) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(venda.getId());
        dto.setDataVenda(venda.getDataVenda());
        dto.setTotalComIva(venda.getTotalComIva());

        if (venda.getCliente() != null) {
            dto.setClienteId(venda.getCliente().getId());
            dto.setClienteNome(venda.getCliente().getNome());
        } else {
            dto.setClienteNome("Consumidor Final");
        }

        // A nossa Lógica de Designação Inteligente
        if (venda.getLinhas() == null || venda.getLinhas().isEmpty()) {
            dto.setDesignacao("Fatura #" + venda.getId());
        } else {
            var primeira = venda.getLinhas().get(0);
            String base = (primeira.getDesignacaoPersonalizada() != null && !primeira.getDesignacaoPersonalizada().trim().isEmpty())
                    ? primeira.getDesignacaoPersonalizada()
                    : primeira.getArtigo().getNome();

            if (venda.getLinhas().size() > 1) {
                int extra = venda.getLinhas().size() - 1;
                dto.setDesignacao(base + " (+ " + extra + " item)");
            } else {
                dto.setDesignacao(base);
            }
        }

        return dto;
    }
}