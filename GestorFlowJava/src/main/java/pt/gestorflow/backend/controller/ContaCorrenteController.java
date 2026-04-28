package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ContaCorrenteExtratoDTO;
import pt.gestorflow.backend.dto.ContaCorrenteResumoDTO; // 🚀 O nosso DTO unificador
import pt.gestorflow.backend.service.ContaCorrenteService;

import java.util.List;

@RestController
@RequestMapping("/api/contas-correntes")
@RequiredArgsConstructor
public class ContaCorrenteController {

    private final ContaCorrenteService service;

    @GetMapping("/clientes/resumo")
    public ResponseEntity<List<ContaCorrenteResumoDTO>> getResumoClientes() {
        // 🚀 Agora devolve o DTO traduzido com as chaves 'nome' e 'totalFaturado'
        return ResponseEntity.ok(service.obterResumoClientes());
    }

    @GetMapping("/clientes/{id}/extrato")
    public ResponseEntity<List<ContaCorrenteExtratoDTO>> getExtratoCliente(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterExtratoCliente(id));
    }

    @GetMapping("/fornecedores/resumo")
    public ResponseEntity<List<ContaCorrenteResumoDTO>> getResumoFornecedores() {
        // 🚀 O mesmo aqui para os Fornecedores!
        return ResponseEntity.ok(service.obterResumoFornecedores());
    }

    @GetMapping("/fornecedores/{id}/extrato")
    public ResponseEntity<List<ContaCorrenteExtratoDTO>> getExtratoFornecedor(@PathVariable Long id) {
        return ResponseEntity.ok(service.obterExtratoFornecedor(id));
    }
}