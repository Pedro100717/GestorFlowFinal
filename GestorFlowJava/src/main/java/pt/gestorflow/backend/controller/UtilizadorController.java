package pt.gestorflow.backend.controller;

import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.service.UtilizadorService;

import java.util.List;

@RestController
@RequestMapping("/api/utilizadores")
@CrossOrigin(origins = "*")
public class UtilizadorController {

    private final UtilizadorService service;

    public UtilizadorController(UtilizadorService service) {
        this.service = service;
    }

    // Apenas para listar (protegido por token)
    @GetMapping
    public List<Utilizador> listar() {
        return service.listarTodos();
    }
}