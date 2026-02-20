package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.model.Venda;
import pt.gestorflow.backend.repository.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VendaRepository vendaRepository;
    private final CompraRepository compraRepository;
    private final ArtigoRepository artigoRepository;
    private final ClienteRepository clienteRepository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public Map<String, Object> getResumo() {
        Utilizador user = getUtilizadorLogado();
        Long userId = user.getId();

        // 1. Total Vendas (Nome no teu VendaRepository: totalVendasReais)
        BigDecimal totalVendas = vendaRepository.totalVendasReais(userId);

        // 2. Total Compras (Nome no teu CompraRepository: totalGastos)
        BigDecimal totalCompras = compraRepository.totalGastos(userId);

        // 3. Valor Stock (Query Nativa do ArtigoRepository)
        BigDecimal valorStock = artigoRepository.valorTotalStock(userId);

        // 4. Total Clientes (Contagem do ClienteRepository)
        long totalClientes = clienteRepository.countByUtilizadorId(userId);

        // 5. Últimas Vendas (Nome no teu VendaRepository)
        List<Venda> ultimasVendas = vendaRepository.findTop5ByUtilizadorIdOrderByDataVendaDesc(userId);

        Map<String, Object> dados = new HashMap<>();
        dados.put("totalVendas", totalVendas);
        dados.put("totalCompras", totalCompras);
        dados.put("valorStock", valorStock);
        dados.put("totalClientes", totalClientes);
        dados.put("ultimasVendas", ultimasVendas);

        return dados;
    }
}