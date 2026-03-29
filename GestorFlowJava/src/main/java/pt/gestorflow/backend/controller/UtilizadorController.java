package pt.gestorflow.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

@RestController
@RequestMapping("/api/utilizadores")
@RequiredArgsConstructor
public class UtilizadorController {

    private final UtilizadorRepository repository;

    @GetMapping("/me")
    public ResponseEntity<PerfilResponseDTO> getPerfil() {
        // Vai buscar o "esqueleto" do utilizador que o nosso Filtro JWT criou
        Utilizador userAutenticado = (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Vai à BD buscar os dados frescos (caso ele tenha mudado o email há 5 minutos)
        Utilizador user = repository.findById(userAutenticado.getId())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        PerfilResponseDTO dto = new PerfilResponseDTO();
        dto.setId(user.getId());
        dto.setNomeUtilizador(user.getNomeUtilizador());
        dto.setEmail(user.getEmail());

        return ResponseEntity.ok(dto);
    }
}