package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor; // Usar esta anotação se quiseres evitar o construtor manual
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.ClienteDTO;
import pt.gestorflow.backend.model.Cliente;
import pt.gestorflow.backend.service.ClienteService;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody ClienteDTO dto) {
        // Se o NIF for inválido, o @Valid dispara e o GlobalExceptionHandler apanha
        // Se o NIF for duplicado, o Service lança erro e o GlobalExceptionHandler apanha
        return ResponseEntity.ok(service.criarCliente(dto));
    }

    @GetMapping
    public ResponseEntity<Page<Cliente>> listar(
            @RequestParam(defaultValue = "0") int page, // Página 0 é a primeira
            @RequestParam(defaultValue = "10") int size // 10 itens por defeito
    ) {
        return ResponseEntity.ok(service.listarMeusClientes(page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        return ResponseEntity.ok(service.atualizarCliente(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}