package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ContaBancariaDTO; // Novo Import
import pt.gestorflow.backend.dto.MovimentoDTO;
import pt.gestorflow.backend.model.ContaBancaria;
import pt.gestorflow.backend.model.Movimento;
import pt.gestorflow.backend.service.TesourariaService;

import java.util.List;

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
    public ResponseEntity<Movimento> criarMovimento(@Valid @RequestBody MovimentoDTO dto) {
        return ResponseEntity.ok(service.registarMovimento(dto));
    }

    @GetMapping("/contas/{id}/extrato")
    public List<Movimento> verExtrato(@PathVariable Long id) {
        return service.obterExtrato(id);
    }
}