package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j // 🚀 Lombok toma conta do recado
@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository repository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public CentroCustoResponseDTO criar(CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ Registo de Auditoria Financeira
        log.info("Início da criação de um novo Centro de Custo ('{}') para o utilizador ID: {}", dto.getNome(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(user);

        CentroCusto salvo = repository.save(cc);
        log.debug("Centro de Custo '{}' criado com sucesso com o ID: {}", salvo.getNome(), salvo.getId());

        return converterParaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de Centros de Custo solicitada pelo utilizador ID: {}", utilizadorId);

        return repository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public CentroCustoResponseDTO atualizar(Long id, CentroCustoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Centro de Custo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());

        CentroCusto atualizado = repository.save(cc);
        log.debug("Centro de Custo ID: {} atualizado com sucesso", atualizado.getId());

        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public CentroCustoResponseDTO buscarPorId(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        return converterParaDTO(cc);
    }

    @Transactional
    public void eliminar(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Aviso Crítico: Pedido de eliminação do Centro de Custo ID: {} pelo utilizador ID: {}", id, utilizadorId);

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        repository.delete(cc);
        log.debug("Centro de Custo ID: {} eliminado com sucesso", id);
    }

    @Transactional
    public List<CentroCustoResponseDTO> importarEmLote(List<CentroCustoDTO> dtos) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar importação em lote de {} centros de custo para o utilizador ID: {}", dtos.size(), utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        List<CentroCusto> centrosParaGuardar = new ArrayList<>();
        Set<String> codigosNoLote = new HashSet<>();

        for (int i = 0; i < dtos.size(); i++) {
            CentroCustoDTO dto = dtos.get(i);
            int linhaReal = i + 1;

            // 🛡️ Validação 1: Nome é estritamente obrigatório
            if (dto.getNome() == null || dto.getNome().trim().isEmpty()) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": O Nome do Centro de Custo é obrigatório.");
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
                while (codigosNoLote.contains(codigoLimpo) || repository.existsByCodigoAndUtilizadorId(codigoLimpo, utilizadorId)) {
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
            if (repository.existsByCodigoAndUtilizadorId(codigoLimpo, utilizadorId)) {
                throw new IllegalArgumentException("Erro na linha " + linhaReal + ": Já existe um Centro de Custo com o Código '" + codigoLimpo + "' na sua conta.");
            }

            CentroCusto cc = new CentroCusto();
            cc.setNome(nomeLimpo);
            cc.setCodigo(codigoLimpo);
            cc.setUtilizador(user);

            centrosParaGuardar.add(cc);
        }

        List<CentroCusto> guardados = repository.saveAll(centrosParaGuardar);
        log.debug("Importação concluída. {} centros de custo guardados.", guardados.size());

        return guardados.stream().map(this::converterParaDTO).toList();
    }

    private CentroCustoResponseDTO converterParaDTO(CentroCusto cc) {
        CentroCustoResponseDTO dto = new CentroCustoResponseDTO();
        dto.setId(cc.getId());
        dto.setNome(cc.getNome());
        dto.setCodigo(cc.getCodigo());
        return dto;
    }
}