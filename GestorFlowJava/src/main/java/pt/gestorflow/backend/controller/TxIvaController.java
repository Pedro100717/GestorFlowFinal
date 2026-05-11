package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.gestorflow.backend.dto.TxIvaResponseDTO; // 🚀 IMPORT ATUALIZADO
import pt.gestorflow.backend.service.TxIvaService;

import java.util.List;

@RestController
@RequestMapping("/api/iva")
@RequiredArgsConstructor
public class TxIvaController {

    private final TxIvaService txIvaService;

    // 🚀 TIPO DE RETORNO ATUALIZADO
    @GetMapping
    public ResponseEntity<List<TxIvaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(txIvaService.listarTodas());
    }
}