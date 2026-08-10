package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.CompraDTO;
import pt.gestorflow.backend.dto.CompraResponseDTO;
import pt.gestorflow.backend.dto.TxIvaResponseDTO; // 🚀 Entra o DTO
import pt.gestorflow.backend.service.CompraService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
@Tag(name = "Compras", description = "Gestão de faturas de fornecedores e compras de mercadoria/serviços")
public class CompraController {

    private final CompraService service;

    // 🛡️ CONTRATO DE AÇO: Fim do try-catch e do <?>. Retorna 201 Created.
    @Operation(summary = "Registar nova compra", description = "Cria uma nova fatura de compra de um fornecedor e atualiza stocks se aplicável.")
    @PostMapping
    public ResponseEntity<CompraResponseDTO> criar(@Valid @RequestBody CompraDTO dto) {
        log.debug("Pedido HTTP POST recebido: /api/compras");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registarCompra(dto));
    }

    // 🛡️ ADICIONADO: O Angular vai precisar disto para abrir a ficha de uma compra passada
    @Operation(summary = "Obter detalhe da compra", description = "Devolve a informação completa de uma compra específica pelo seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> buscarPorId(@PathVariable Long id) {
        log.debug("Pedido HTTP GET recebido: /api/compras/{}", id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar compras paginadas", description = "Devolve o histórico de compras do utilizador autenticado, com paginação.")
    @GetMapping
    public ResponseEntity<Page<CompraResponseDTO>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("Pedido HTTP GET recebido: /api/compras (Página: {}, Tamanho: {})", page, size);
        return ResponseEntity.ok(service.listarMinhasCompras(page, size));
    }

    @Operation(summary = "Atualizar compra", description = "Edita os dados de uma compra existente que ainda não possua pagamentos na tesouraria.")
    @PutMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> atualizarCompra(@PathVariable Long id, @Valid @RequestBody CompraDTO dto) {
        log.debug("Pedido HTTP PUT recebido: /api/compras/{}", id);
        return ResponseEntity.ok(service.atualizarCompra(id, dto));
    }

    @Operation(summary = "Eliminar compra", description = "Anula uma compra e repõe os stocks das mercadorias envolvidas.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.debug("Pedido HTTP DELETE recebido: /api/compras/{}", id);
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // 🛡️ CORREÇÃO INDUSTRIAL: O Controller agora cospe APENAS DTOs!
    @Operation(summary = "Listar Taxas de IVA", description = "Devolve a lista de taxas de IVA globais disponíveis no sistema.")
    @GetMapping("/taxas-iva")
    public ResponseEntity<List<TxIvaResponseDTO>> listarTaxasIva() {
        log.debug("Pedido HTTP GET recebido: /api/compras/taxas-iva");

        List<TxIvaResponseDTO> taxasDTO = service.listarTaxasIva().stream()
                .map(iva -> new TxIvaResponseDTO(iva.getId(), iva.getDescricao(), iva.getValor()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(taxasDTO);
    }
}