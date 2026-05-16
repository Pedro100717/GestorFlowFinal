package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

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
    // --- GESTÃO CRUD SIMPLIFICADA (CASH FLOW PURO SEM IVA) ---
    // =========================================================================

    @Transactional
    public MovimentoPlaneadoDTO criarPlano(MovimentoPlaneadoDTO dto) {
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

        return dto;
    }
}