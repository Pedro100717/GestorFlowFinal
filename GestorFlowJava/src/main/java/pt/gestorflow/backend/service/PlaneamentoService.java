package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class PlaneamentoService {

    private final MovimentoPlaneadoRepository planeamentoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;

    // =========================================================================
    // --- GESTÃO CRUD SIMPLIFICADA E BLINDADA ---
    // =========================================================================

    @Transactional
    public MovimentoPlaneadoDTO criarPlano(MovimentoPlaneadoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("A iniciar criação de um novo plano financeiro ({}) para o utilizador ID: {}", dto.getFrequencia(), utilizadorId);

        // 🚀 GUARDIÃO AGORA COM CONTEXTO: Passamos o ID para rastrear quem falha a validação
        validarPlano(dto, utilizadorId);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        MovimentoPlaneado plano = new MovimentoPlaneado();
        plano.setUtilizador(user);
        mapearDtoParaEntidade(dto, plano, utilizadorId);

        MovimentoPlaneado salvo = planeamentoRepository.save(plano);
        log.debug("Plano financeiro ID: {} criado com sucesso.", salvo.getId());

        return mapearEntidadeParaDto(salvo);
    }

    @Transactional
    public MovimentoPlaneadoDTO atualizarPlano(Long id, MovimentoPlaneadoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Pedido de atualização do Plano ID: {} pelo utilizador ID: {}", id, utilizadorId);

        validarPlano(dto, utilizadorId);

        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        mapearDtoParaEntidade(dto, plano, utilizadorId);

        MovimentoPlaneado atualizado = planeamentoRepository.save(plano);
        log.debug("Plano financeiro ID: {} atualizado com sucesso.", atualizado.getId());

        return mapearEntidadeParaDto(atualizado);
    }

    @Transactional(readOnly = true)
    public List<MovimentoPlaneadoDTO> listarPlanos() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("Listagem de planeamento solicitada pelo utilizador ID: {}", utilizadorId);

        return planeamentoRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::mapearEntidadeParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void apagarPlano(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria Crítica: Pedido de eliminação definitiva do Plano ID: {} (Utilizador: {})", id, utilizadorId);

        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        planeamentoRepository.delete(plano);
        log.debug("Plano ID: {} eliminado com sucesso da base de dados.", id);
    }

    @Transactional
    public void alternarStatus(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        plano.setAtivo(!plano.getAtivo());

        // 🛡️ INFO: Registo essencial para justificar quebras no Cash Flow
        log.info("Auditoria: O utilizador ID: {} alterou o status do Plano ID: {} para {}", utilizadorId, id, plano.getAtivo() ? "ATIVO" : "INATIVO");

        planeamentoRepository.save(plano);
    }

    // =========================================================================
    // --- REGRAS DE NEGÓCIO (VALIDAÇÕES) ---
    // =========================================================================

    private void validarPlano(MovimentoPlaneadoDTO dto, Long utilizadorId) {
        if (dto.getFrequencia() != FrequenciaMovimento.PONTUAL && dto.getDataFim() == null) {
            log.warn("Validação falhou: Utilizador {} tentou criar plano recorrente sem data de fim.", utilizadorId);
            throw new IllegalArgumentException("A data de fim é obrigatória para movimentos recorrentes.");
        }

        if (dto.getFrequencia() == FrequenciaMovimento.PONTUAL && dto.getDataFim() != null) {
            dto.setDataFim(null);
        }

        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            log.warn("Validação falhou: Utilizador {} definiu data de fim anterior à data de início.", utilizadorId);
            throw new IllegalArgumentException("A data de término não pode ser anterior à data de início.");
        }
    }

    // =========================================================================
    // --- MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR) 🚀 ---
    // =========================================================================

    @Transactional
    public void ignorarDataPlano(Long id, LocalDate dataAignorar) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ INFO: Rastreio de manipulação de previsão de tesouraria
        log.info("Auditoria Financeira: Utilizador ID: {} pediu para ignorar a data {} no Plano recorrente ID: {}", utilizadorId, dataAignorar, id);

        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        plano.getDatasIgnoradas().add(dataAignorar);
        planeamentoRepository.save(plano);
    }

    @Transactional
    public MovimentoPlaneadoDTO criarExcecaoPlano(Long idOriginal, LocalDate dataOriginal, MovimentoPlaneadoDTO dtoNovo) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("Auditoria Financeira: Utilizador ID: {} a criar exceção no Plano ID: {} para a data {}", utilizadorId, idOriginal, dataOriginal);

        MovimentoPlaneado planoOriginal = planeamentoRepository.findByIdAndUtilizadorId(idOriginal, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano original não encontrado."));

        planoOriginal.getDatasIgnoradas().add(dataOriginal);
        planeamentoRepository.save(planoOriginal);

        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        MovimentoPlaneado novoPlanoExcecao = new MovimentoPlaneado();
        novoPlanoExcecao.setUtilizador(user);
        mapearDtoParaEntidade(dtoNovo, novoPlanoExcecao, utilizadorId);

        novoPlanoExcecao.setFrequencia(FrequenciaMovimento.PONTUAL);
        novoPlanoExcecao.setDataInicio(dtoNovo.getDataInicio());
        novoPlanoExcecao.setDataFim(null);

        MovimentoPlaneado excecaoSalva = planeamentoRepository.save(novoPlanoExcecao);
        log.debug("Exceção criada com sucesso. Novo Plano Pontual ID: {}", excecaoSalva.getId());

        return mapearEntidadeParaDto(excecaoSalva);
    }

    // =========================================================================
    // --- MAPEAMENTOS ---
    // =========================================================================

    private void mapearDtoParaEntidade(MovimentoPlaneadoDTO dto, MovimentoPlaneado plano, Long utilizadorId) {
        plano.setDescricao(dto.getDescricao());
        plano.setTipo(dto.getTipo());
        plano.setFrequencia(dto.getFrequencia());
        plano.setValorBase(dto.getValorBase());
        plano.setDataInicio(dto.getDataInicio());
        plano.setDataFim(dto.getDataFim());
        plano.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        if (dto.getClienteId() != null) {
            plano.setCliente(clienteRepository.findByIdAndUtilizadorId(dto.getClienteId(), utilizadorId).orElse(null));
        } else {
            plano.setCliente(null);
        }

        if (dto.getFornecedorId() != null) {
            plano.setFornecedor(fornecedorRepository.findByIdAndUtilizadorId(dto.getFornecedorId(), utilizadorId).orElse(null));
        } else {
            plano.setFornecedor(null);
        }
    }

    private MovimentoPlaneadoDTO mapearEntidadeParaDto(MovimentoPlaneado plano) {
        MovimentoPlaneadoDTO dto = new MovimentoPlaneadoDTO();
        dto.setId(plano.getId());
        dto.setDescricao(plano.getDescricao());
        dto.setTipo(plano.getTipo());
        dto.setFrequencia(plano.getFrequencia());
        dto.setValorBase(plano.getValorBase());
        dto.setDataInicio(plano.getDataInicio());
        dto.setDataFim(plano.getDataFim());
        dto.setAtivo(plano.getAtivo());
        dto.setDataUltimoProcessamento(plano.getDataUltimoProcessamento());

        if (plano.getCliente() != null) {
            dto.setClienteId(plano.getCliente().getId());
        }
        if (plano.getFornecedor() != null) {
            dto.setFornecedorId(plano.getFornecedor().getId());
        }

        dto.setDatasIgnoradas(plano.getDatasIgnoradas());

        return dto;
    }
}