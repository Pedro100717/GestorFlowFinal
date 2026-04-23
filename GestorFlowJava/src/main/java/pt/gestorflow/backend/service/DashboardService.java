package pt.gestorflow.backend.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.DashboardDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.model.Venda;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final VendaRepository vendaRepository;
    private final CompraRepository compraRepository;
    private final ArtigoRepository artigoRepository;
    private final ClienteRepository clienteRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 🛡️ CORREÇÃO: O retorno agora é o DTO blindado, não um Map solto
    public DashboardDTO getResumo() {
        Utilizador user = getUtilizadorLogado();
        Long userId = user.getId();

        // NOTA DE PERFORMANCE INDUSTRIAL:
        // Somar a BD inteira (totalVendasReais) vai causar lentidão grave
        // quando tiveres milhares de registos. No futuro, pondera passar datas (ex: Mês Atual).
        BigDecimal totalVendas = vendaRepository.totalVendasReais(userId);
        BigDecimal totalCompras = compraRepository.totalGastos(userId);
        BigDecimal valorStock = artigoRepository.valorTotalStock(userId);
        long totalClientes = clienteRepository.countByUtilizadorId(userId);

        List<Venda> ultimasVendas = vendaRepository.findTop5ByUtilizadorIdOrderByDataVendaDesc(userId);

        // 🛡️ Mapeamento Limpo e Tipado
        List<VendaResponseDTO> ultimasVendasDTO = ultimasVendas.stream()
                .map(this::converterVendaParaDTO)
                .toList();

        return DashboardDTO.builder()
                .totalVendas(totalVendas)
                .totalCompras(totalCompras)
                .valorStock(valorStock)
                .totalClientes(totalClientes)
                .ultimasVendas(ultimasVendasDTO)
                .build();
    }

    // Usamos um conversor simplificado só para o Dashboard (podes reutilizar o do VendaService se preferires partilhar lógica)
    private VendaResponseDTO converterVendaParaDTO(Venda venda) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(venda.getId());
        dto.setDataVenda(venda.getDataVenda());
        dto.setTotalComIva(venda.getTotalComIva());

        if (venda.getCliente() != null) {
            dto.setClienteId(venda.getCliente().getId());
            dto.setClienteNome(venda.getCliente().getNome());
            // 🛡️ ADICIONADO: Criar uma designação virtual para o ecrã do Dashboard não ficar vazio
            dto.setDesignacao("Faturação #" + venda.getId() + " - " + venda.getCliente().getNome());
        } else {
            dto.setClienteNome("Consumidor Final");
            dto.setDesignacao("Faturação #" + venda.getId() + " - Consumidor Final");
        }

        return dto;
    }
}