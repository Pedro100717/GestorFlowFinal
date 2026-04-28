package pt.gestorflow.backend.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.DashboardDTO;
import pt.gestorflow.backend.dto.VendaResponseDTO;
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
    private final AuthService authService; // 🚀 A nossa Chave Mestra de Segurança

    public DashboardDTO getResumo() {
        // 🚀 Obtém o ID blindado a partir do Token JWT
        Long userId = authService.getUtilizadorAutenticadoId();

        // Executa as queries de agregação filtradas pelo Utilizador
        BigDecimal totalVendas = vendaRepository.totalVendasReais(userId);
        BigDecimal totalCompras = compraRepository.totalGastos(userId);
        BigDecimal valorStock = artigoRepository.valorTotalStock(userId);
        long totalClientes = clienteRepository.countByUtilizadorId(userId);

        List<Venda> ultimasVendas = vendaRepository.findTop5ByUtilizadorIdOrderByDataVendaDesc(userId);

        // Mapeamento para DTO para transporte seguro de dados
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

    private VendaResponseDTO converterVendaParaDTO(Venda venda) {
        VendaResponseDTO dto = new VendaResponseDTO();
        dto.setId(venda.getId());
        dto.setDataVenda(venda.getDataVenda());
        dto.setTotalComIva(venda.getTotalComIva());

        if (venda.getCliente() != null) {
            dto.setClienteId(venda.getCliente().getId());
            dto.setClienteNome(venda.getCliente().getNome());
            dto.setDesignacao("Faturação #" + venda.getId() + " - " + venda.getCliente().getNome());
        } else {
            dto.setClienteNome("Consumidor Final");
            dto.setDesignacao("Faturação #" + venda.getId() + " - Consumidor Final");
        }

        return dto;
    }
}