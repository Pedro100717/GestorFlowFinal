package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@Tag(name = "Telemetria", description = "Receção de logs do Frontend Angular")
public class LogController {

    @Operation(summary = "Receber logs do browser", description = "Absorve os erros e eventos enviados pelo LogService do Angular para não quebrar a UI.")
    @PostMapping("/frontend")
    public ResponseEntity<Void> receberLogDoFrontend(@RequestBody Map<String, Object> logPayload) {

        // Imprime o log que veio do Angular diretamente na consola do teu Docker
        log.warn("📡 [LOG DO FRONTEND]: {}", logPayload);

        // Devolve um 200 OK silencioso para o Angular ficar feliz e continuar a trabalhar
        return ResponseEntity.ok().build();
    }
}