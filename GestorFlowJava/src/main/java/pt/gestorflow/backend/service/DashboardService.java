package pt.gestorflow.backend.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import pt.gestorflow.backend.dto.DashboardDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.Venda;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j // 🚀 Anotação Mágica
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

        LocalDate dataInicioSegura = (inicio != null) ? inicio : LocalDate.of(2000, 1, 1);
        LocalDate dataFimSegura = (fim != null) ? fim : LocalDate.of(2100, 12, 31);

        // 🛡️ DEBUG: Registo leve para garantir que os filtros de datas estão a passar bem do Angular para o Spring
        log.debug("A gerar Dashboard para utilizador ID: {} (Período: {} a {})", userId, dataInicioSegura, dataFimSegura);

        BigDecimal totalCompras = compraRepository.totalGastos(userId, dataInicioSegura, dataFimSegura);
        BigDecimal totalVendas = vendaRepository.totalVendasReais(userId, dataInicioSegura, dataFimSegura);

        BigDecimal vendasBase = vendaRepository.totalVendasBase(userId, dataInicioSegura, dataFimSegura);
        BigDecimal comprasBase = compraRepository.totalComprasBase(userId, dataInicioSegura, dataFimSegura);

        // 🚀 CORREÇÃO CRÍTICA: Prevenir NullPointerException nas operações matemáticas
        BigDecimal vendasSeguras = vendasBase != null ? vendasBase : BigDecimal.ZERO;
        BigDecimal comprasSeguras = comprasBase != null ? comprasBase : BigDecimal.ZERO;
        BigDecimal margemBruta = vendasSeguras.subtract(comprasSeguras);

        BigDecimal valorStock = artigoRepository.valorTotalStock(userId);

        List<Venda> ultimasVendas = vendaRepository.findRecentVendas(userId, dataInicioSegura, dataFimSegura, PageRequest.of(0, 5));

        List<VendaResponseDTO> ultimasVendasDTO = ultimasVendas.stream()
                .map(this::converterVendaParaDTO)
                .toList();

        log.debug("Dashboard gerado com sucesso. Margem bruta: {}", margemBruta);

        return DashboardDTO.builder()
                .totalVendas(totalVendas != null ? totalVendas : BigDecimal.ZERO)
                .totalCompras(totalCompras != null ? totalCompras : BigDecimal.ZERO)
                .valorStock(valorStock != null ? valorStock : BigDecimal.ZERO)
                .margemBruta(margemBruta) // Já vem protegida de cima
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