package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.SeccaoHomoDTO;
import pt.gestorflow.backend.dto.SeccaoHomoResponseDTO;
import pt.gestorflow.backend.model.SeccaoHomo;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.SeccaoHomoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class SeccaoHomoService {

    private final SeccaoHomoRepository seccaoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public SeccaoHomoResponseDTO criar(SeccaoHomoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de nova Secção Homogénea ('{}') para o utilizador ID: {}", dto.getNome(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        SeccaoHomo sh = new SeccaoHomo();
        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());
        sh.setUtilizador(user);

        SeccaoHomo salva = seccaoRepository.save(sh); // 🚀 Variável chama-se "salva"
        log.debug("Secção Homogénea criada com sucesso com o ID: {}", salva.getId());

        return converterParaDTO(salva); // 🚀 Corrigido aqui para "salva" também!
    }

    @Transactional(readOnly = true)
    public List<SeccaoHomoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de Secções Homogéneas solicitada pelo utilizador ID: {}", utilizadorId);

        return seccaoRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public SeccaoHomoResponseDTO atualizar(Long id, SeccaoHomoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização da Secção Homogénea ID: {} pelo utilizador ID: {}", id, utilizadorId);

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção não encontrada ou acesso negado."));

        sh.setNome(dto.getNome());
        sh.setCodigo(dto.getCodigo());

        SeccaoHomo atualizada = seccaoRepository.save(sh);
        log.debug("Secção Homogénea ID: {} atualizada com sucesso.", atualizada.getId());

        return converterParaDTO(atualizada);
    }

    @Transactional(readOnly = true)
    public SeccaoHomoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

        return converterParaDTO(sh);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria: Pedido de eliminação da Secção Homogénea ID: {} pelo utilizador ID: {}", id, utilizadorId);

        SeccaoHomo sh = seccaoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea não encontrada ou acesso negado."));

        seccaoRepository.delete(sh);
        log.debug("Secção Homogénea ID: {} eliminada com sucesso.", id);
    }

    @Transactional
    public List<SeccaoHomoResponseDTO> importarEmLote(List<SeccaoHomoDTO> dtos) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar importação em lote de {} secções homogéneas para o utilizador ID: {}", dtos.size(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        List<SeccaoHomo> seccoesParaGuardar = new ArrayList<>();
        Set<String> codigosNoLote = new HashSet<>();

        for (int i = 0; i < dtos.size(); i++) {
            SeccaoHomoDTO dto = dtos.get(i);
            int linhaReal = i + 1;

            // 🛡️ Validação 1: Nome é estritamente obrigatório
            if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O Nome da Secção é obrigatório.");
            }

            String nomeLimpo = dto.getNome().trim();
            String codigoLimpo;

            // 🛡️ SMART FALLBACK: Se o código vier vazio, geramos a partir do nome
            if (dto.getCodigo() == null || dto.getCodigo().trim().isEmpty()) {
                // Remove acentos, substitui espaços e carateres especiais por _, e converte para maiúsculas
                String baseCodigo = nomeLimpo.toUpperCase()
                        .replaceAll("[ÁÀÂÃ]", "A")
                        .replaceAll("[ÉÊ]", "E")
                        .replaceAll("[Í]", "I")
                        .replaceAll("[ÓÔÕ]", "O")
                        .replaceAll("[Ú]", "U")
                        .replaceAll("[Ç]", "C")
                        .replaceAll("[^A-Z0-9]", "_")
                        .replaceAll("_+", "_");

                if (baseCodigo.length() > 15) {
                    baseCodigo = baseCodigo.substring(0, 15);
                }
                if (baseCodigo.endsWith("_")) {
                    baseCodigo = baseCodigo.substring(0, baseCodigo.length() - 1);
                }

                codigoLimpo = baseCodigo;
                int contador = 1;

                // Evita colisões tanto no próprio ficheiro Excel como na Base de Dados
                while (codigosNoLote.contains(codigoLimpo) || seccaoRepository.existsByCodigoAndUtilizadorId(codigoLimpo, utilizadorId)) {
                    contador++;
                    String sufixo = "_" + contador;
                    int maxBaseLen = 15 - sufixo.length();
                    codigoLimpo = (baseCodigo.length() > maxBaseLen ? baseCodigo.substring(0, maxBaseLen) : baseCodigo) + sufixo;
                }

            } else {
                codigoLimpo = dto.getCodigo().trim();
            }

            // 🛡️ Validação 2: Código duplicado no próprio ficheiro Excel
            if (!codigosNoLote.add(codigoLimpo)) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O Código '" + codigoLimpo + "' está repetido no ficheiro.");
            }

            // 🛡️ Validação 3: Código já existe na Base de Dados?
            if (seccaoRepository.existsByCodigoAndUtilizadorId(codigoLimpo, utilizadorId)) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": Já existe uma Secção com o Código '" + codigoLimpo + "' na sua conta.");
            }

            SeccaoHomo sh = new SeccaoHomo();
            sh.setNome(nomeLimpo);
            sh.setCodigo(codigoLimpo);
            sh.setUtilizador(user);

            seccoesParaGuardar.add(sh);
        }

        List<SeccaoHomo> guardadas = seccaoRepository.saveAll(seccoesParaGuardar);
        log.debug("Importação concluída. {} secções guardadas.", guardadas.size());

        return guardadas.stream().map(this::converterParaDTO).toList();
    }

    private SeccaoHomoResponseDTO converterParaDTO(SeccaoHomo sh) {
        SeccaoHomoResponseDTO dto = new SeccaoHomoResponseDTO();
        dto.setId(sh.getId());
        dto.setNome(sh.getNome());
        dto.setCodigo(sh.getCodigo());
        return dto;
    }
}