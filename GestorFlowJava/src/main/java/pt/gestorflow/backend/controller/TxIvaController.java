package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.TxIvaResponseDTO;
import pt.gestorflow.backend.service.TxIvaService;

import java.util.List;

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/iva")
@RequiredArgsConstructor
@Tag(name = "Taxas de IVA", description = "Consulta das taxas de imposto aplicáveis a artigos e serviços")
public class TxIvaController {

    private final TxIvaService txIvaService;

    // 🚀 TIPO DE RETORNO ATUALIZADO
    @Operation(summary = "Listar Taxas de IVA", description = "Devolve a lista completa de taxas de IVA (ex: Normal, Intermédia, Reduzida) ativas no sistema.")
    @GetMapping
    public ResponseEntity<List<TxIvaResponseDTO>> listarTodas() {
        log.debug("Pedido HTTP GET recebido: /api/iva");
        return ResponseEntity.ok(txIvaService.listarTodas());
    }
}