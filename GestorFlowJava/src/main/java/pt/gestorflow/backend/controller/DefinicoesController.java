package pt.gestorflow.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.gestorflow.backend.dto.EmpresaDTO;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.dto.PerfilUtilizadorDTO;
import pt.gestorflow.backend.service.EmpresaService;
import pt.gestorflow.backend.service.UtilizadorService;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.Collections;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/definicoes")
@RequiredArgsConstructor
public class DefinicoesController {

    private final EmpresaService empresaService;
    private final UtilizadorService utilizadorService;

    // ==========================================
    // ENDPOINTS DA EMPRESA (ENTIDADE FISCAL)
    // ==========================================

    @GetMapping("/empresa")
    public ResponseEntity<EmpresaDTO> obterConfiguracoesEmpresa() {
        return ResponseEntity.ok(empresaService.obterConfiguracoesAtuais());
    }

    @PutMapping("/empresa")
    public ResponseEntity<Void> guardarConfiguracoesEmpresa(@Valid @RequestBody EmpresaDTO dto) {
        empresaService.guardarConfiguracoes(dto);
        return ResponseEntity.ok().build();
    }

    // 🚀 NOVO ENDPOINT: UPLOAD DE LOGOTIPO
    @PostMapping(value = "/empresa/logo", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> fazerUploadLogotipo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new IllegalArgumentException("O ficheiro está vazio.");

        try {
            Path diretoria = Paths.get("uploads/logos/");
            if (!Files.exists(diretoria)) {
                Files.createDirectories(diretoria);
            }

            String nomeFicheiro = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            Path caminhoFicheiro = diretoria.resolve(nomeFicheiro);

            Files.copy(file.getInputStream(), caminhoFicheiro, StandardCopyOption.REPLACE_EXISTING);

            String caminhoUrl = "/uploads/logos/" + nomeFicheiro;
            empresaService.guardarCaminhoLogo(caminhoUrl);

            return ResponseEntity.ok(Collections.singletonMap("caminho", caminhoUrl));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Erro ao guardar o logotipo.", e);
        }
    }

    // ==========================================
    // ENDPOINTS DO UTILIZADOR (IDENTIDADE)
    // ==========================================

    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> obterPerfil() {
        return ResponseEntity.ok(utilizadorService.obterPerfil());
    }

    @PutMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> atualizarPerfil(@Valid @RequestBody PerfilUtilizadorDTO dto) {
        return ResponseEntity.ok(utilizadorService.atualizarPerfil(dto));
    }
}