package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ContaBancariaDTO;
import pt.gestorflow.backend.dto.MovimentoDTO;
import pt.gestorflow.backend.dto.MovimentoResponseDTO; // NOVO IMPORT
import pt.gestorflow.backend.dto.TransferenciaDTO;
import pt.gestorflow.backend.model.ContaBancaria;
import pt.gestorflow.backend.service.TesourariaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tesouraria")
@RequiredArgsConstructor
public class TesourariaController {

    private final TesourariaService service;

    @PostMapping("/contas")
    public ResponseEntity<ContaBancaria> criarConta(@Valid @RequestBody ContaBancariaDTO dto) {
        return ResponseEntity.ok(service.criarConta(dto));
    }

    @GetMapping("/contas")
    public List<ContaBancaria> listarContas() {
        return service.listarContas();
    }

    @PostMapping("/movimentos")
    public ResponseEntity<MovimentoResponseDTO> criarMovimento(@Valid @RequestBody MovimentoDTO dto) {
        return ResponseEntity.ok(service.registarMovimento(dto));
    }

    @GetMapping("/contas/{id}/extrato")
    public List<MovimentoResponseDTO> verExtrato(@PathVariable Long id) {
        return service.obterExtrato(id);
    }

    @PostMapping("/transferencias")
    public ResponseEntity<?> realizarTransferencia(@Valid @RequestBody TransferenciaDTO dto) {
        service.transferirEntreContas(dto);
        return ResponseEntity.ok().body(Map.of("mensagem", "Transferência realizada com sucesso!"));
    }
}