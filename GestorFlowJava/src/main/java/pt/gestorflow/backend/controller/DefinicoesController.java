package pt.gestorflow.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag; // 🚀 Documentação OpenAPI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.http.MediaType;
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

@Slf4j // 🚀 Telemetria de entrada
@RestController
@RequestMapping("/api/definicoes")
@RequiredArgsConstructor
@Tag(name = "Definições", description = "Configurações globais da empresa e gestão do perfil do utilizador autenticado")
public class DefinicoesController {

    private final EmpresaService empresaService;
    private final UtilizadorService utilizadorService;

    // ==========================================
    // ENDPOINTS DA EMPRESA (ENTIDADE FISCAL)
    // ==========================================

    @Operation(summary = "Obter configurações da Empresa", description = "Devolve os dados fiscais e o logotipo da empresa para faturas e documentos.")
    @GetMapping("/empresa")
    public ResponseEntity<EmpresaDTO> obterConfiguracoesEmpresa() {
        log.debug("Pedido HTTP GET recebido: /api/definicoes/empresa");
        return ResponseEntity.ok(empresaService.obterConfiguracoesAtuais());
    }

    @Operation(summary = "Atualizar configurações da Empresa", description = "Guarda as alterações aos dados fiscais da empresa.")
    @PutMapping("/empresa")
    public ResponseEntity<Void> guardarConfiguracoesEmpresa(@Valid @RequestBody EmpresaDTO dto) {
        log.info("Auditoria: Pedido HTTP PUT recebido para atualizar os dados da Empresa (NIF: {})", dto.getNif());
        empresaService.guardarConfiguracoes(dto);
        return ResponseEntity.ok().build();
    }

    // 🚀 NOVO ENDPOINT: UPLOAD DE LOGOTIPO
    @Operation(summary = "Upload do Logotipo", description = "Recebe um ficheiro de imagem e guarda-o no servidor, atualizando o caminho nas configurações.")
    @PostMapping(value = "/empresa/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> fazerUploadLogotipo(@RequestParam("file") MultipartFile file) {
        log.info("Pedido HTTP POST recebido: Upload de novo Logotipo. Ficheiro: {}, Tamanho: {} bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro enviado está vazio ou corrompido.");
        }

        try {
            // 💡 NOTA: Num refactoring futuro, esta lógica de I/O deve ir para um FileStorageService
            Path diretoria = Paths.get("uploads/logos/");
            if (!Files.exists(diretoria)) {
                Files.createDirectories(diretoria);
            }

            String nomeFicheiro = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            Path caminhoFicheiro = diretoria.resolve(nomeFicheiro);

            Files.copy(file.getInputStream(), caminhoFicheiro, StandardCopyOption.REPLACE_EXISTING);

            String caminhoUrl = "/uploads/logos/" + nomeFicheiro;
            empresaService.guardarCaminhoLogo(caminhoUrl);

            log.debug("Logotipo guardado com sucesso no caminho físico: {}", caminhoFicheiro.toAbsolutePath());

            return ResponseEntity.ok(Collections.singletonMap("caminho", caminhoUrl));

        } catch (java.io.IOException e) {
            log.error("Falha ao escrever o ficheiro de logotipo no disco rígido.", e);
            // 🚀 CORREÇÃO: Usar IllegalStateException para o GlobalExceptionHandler apanhar
            throw new IllegalStateException("Ocorreu um erro ao guardar a imagem no servidor. Verifique as permissões de pasta.", e);
        }
    }

    // ==========================================
    // ENDPOINTS DO UTILIZADOR (IDENTIDADE)
    // ==========================================

    @Operation(summary = "Obter perfil de Utilizador", description = "Devolve a informação de identidade da conta com sessão iniciada.")
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> obterPerfil() {
        log.debug("Pedido HTTP GET recebido: /api/definicoes/perfil");
        return ResponseEntity.ok(utilizadorService.obterPerfil());
    }

    @Operation(summary = "Atualizar perfil", description = "Altera os dados de acesso (Email, Nome) do utilizador autenticado.")
    @PutMapping("/perfil")
    public ResponseEntity<PerfilResponseDTO> atualizarPerfil(@Valid @RequestBody PerfilUtilizadorDTO dto) {
        log.info("Pedido HTTP PUT recebido: /api/definicoes/perfil (Novo Email pretendido: {})", dto.getEmail());
        return ResponseEntity.ok(utilizadorService.atualizarPerfil(dto));
    }
}