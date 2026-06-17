package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
        // 🚀 O NOSSO GUARDIÃO: Valida as regras de negócio antes de fazer qualquer coisa!
        validarPlano(dto);

        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        MovimentoPlaneado plano = new MovimentoPlaneado();
        plano.setUtilizador(user);
        mapearDtoParaEntidade(dto, plano, utilizadorId);

        return mapearEntidadeParaDto(planeamentoRepository.save(plano));
    }

    @Transactional
    public MovimentoPlaneadoDTO atualizarPlano(Long id, MovimentoPlaneadoDTO dto) {
        // 🚀 O NOSSO GUARDIÃO: Valida as regras também na edição!
        validarPlano(dto);

        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        mapearDtoParaEntidade(dto, plano, utilizadorId);
        return mapearEntidadeParaDto(planeamentoRepository.save(plano));
    }

    @Transactional(readOnly = true)
    public List<MovimentoPlaneadoDTO> listarPlanos() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        return planeamentoRepository.findAllByUtilizadorId(utilizadorId)
                .stream()
                .map(this::mapearEntidadeParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void apagarPlano(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));
        planeamentoRepository.delete(plano);
    }

    @Transactional
    public void alternarStatus(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        plano.setAtivo(!plano.getAtivo());
        planeamentoRepository.save(plano);
    }

    // =========================================================================
    // --- REGRAS DE NEGÓCIO (VALIDAÇÕES) ---
    // =========================================================================

    private void validarPlano(MovimentoPlaneadoDTO dto) {
        // 1. SE NÃO FOR PONTUAL, A DATA DE FIM É OBRIGATÓRIA
        if (dto.getFrequencia() != FrequenciaMovimento.PONTUAL && dto.getDataFim() == null) {
            throw new IllegalArgumentException("A data de fim é obrigatória para movimentos recorrentes.");
        }

        // 2. SEGURANÇA EXTRA: Se for pontual, garante que ninguém injeta uma data de fim por malícia
        if (dto.getFrequencia() == FrequenciaMovimento.PONTUAL && dto.getDataFim() != null) {
            dto.setDataFim(null);
        }

        // BÓNUS: Validar se a data de fim não é anterior à data de início!
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new IllegalArgumentException("A data de término não pode ser anterior à data de início.");
        }
    }

    // =========================================================================
    // --- MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR) 🚀 ---
    // =========================================================================

    @Transactional
    public void ignorarDataPlano(Long id, LocalDate dataAignorar) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(id, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        // Guarda a data na gaveta das exceções
        plano.getDatasIgnoradas().add(dataAignorar);
        planeamentoRepository.save(plano);
    }

    @Transactional
    public MovimentoPlaneadoDTO criarExcecaoPlano(Long idOriginal, LocalDate dataOriginal, MovimentoPlaneadoDTO dtoNovo) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 1. Silencia o plano original APENAS naquela data
        MovimentoPlaneado planoOriginal = planeamentoRepository.findByIdAndUtilizadorId(idOriginal, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano original não encontrado."));

        planoOriginal.getDatasIgnoradas().add(dataOriginal);
        planeamentoRepository.save(planoOriginal);

        // 2. Cria a nova exceção como um plano independente e PONTUAL
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        MovimentoPlaneado novoPlanoExcecao = new MovimentoPlaneado();
        novoPlanoExcecao.setUtilizador(user);
        mapearDtoParaEntidade(dtoNovo, novoPlanoExcecao, utilizadorId);

        // Regra de Ferro: A exceção tem de ser pontual, senão criávamos um loop infinito de fantasmas!
        novoPlanoExcecao.setFrequencia(FrequenciaMovimento.PONTUAL);
        novoPlanoExcecao.setDataInicio(dtoNovo.getDataInicio());
        novoPlanoExcecao.setDataFim(null); // Segurança: Pontual não tem fim

        return mapearEntidadeParaDto(planeamentoRepository.save(novoPlanoExcecao));
    }


    // =========================================================================
    // --- MAPEAMENTOS (BUG CORRIGIDO E SEM IVA) ---
    // =========================================================================

    private void mapearDtoParaEntidade(MovimentoPlaneadoDTO dto, MovimentoPlaneado plano, Long utilizadorId) {
        plano.setDescricao(dto.getDescricao());
        plano.setTipo(dto.getTipo());
        plano.setFrequencia(dto.getFrequencia());
        plano.setValorBase(dto.getValorBase());
        plano.setDataInicio(dto.getDataInicio());
        plano.setDataFim(dto.getDataFim());
        plano.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // 🛡️ Os parceiros são mapeados à entrada
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

        // 🚀 AQUI ESTÁ A LINHA QUE TE FALTOU: Transportar as exceções para o Angular!
        dto.setDatasIgnoradas(plano.getDatasIgnoradas());

        return dto;
    }
}