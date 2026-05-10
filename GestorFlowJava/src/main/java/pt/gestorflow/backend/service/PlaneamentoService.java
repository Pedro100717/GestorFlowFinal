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
    private final CentroCustoRepository centroCustoRepository;
    private final SeccaoHomoRepository seccaoHomoRepository;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional
    public MovimentoPlaneadoDTO criarPlano(MovimentoPlaneadoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Utilizador user = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));

        MovimentoPlaneado plano = new MovimentoPlaneado();
        mapearDtoParaEntidade(dto, plano, utilizadorId);
        plano.setUtilizador(user);

        return mapearEntidadeParaDto(planeamentoRepository.save(plano));
    }

    @Transactional
    public MovimentoPlaneadoDTO atualizarPlano(Long id, MovimentoPlaneadoDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findById(id)
                .filter(p -> p.getUtilizador().getId().equals(utilizadorId))
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        mapearDtoParaEntidade(dto, plano, utilizadorId);

        return mapearEntidadeParaDto(planeamentoRepository.save(plano));
    }

    @Transactional(readOnly = true)
    public List<MovimentoPlaneadoDTO> listarPlanos() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        return planeamentoRepository.findAllByUtilizadorIdAndAtivoTrue(utilizadorId)
                .stream()
                .map(this::mapearEntidadeParaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void apagarPlano(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findById(id)
                .filter(p -> p.getUtilizador().getId().equals(utilizadorId))
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        planeamentoRepository.delete(plano);
    }

    @Transactional
    public void alternarStatus(Long id) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findById(id)
                .filter(p -> p.getUtilizador().getId().equals(utilizadorId))
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        plano.setAtivo(!plano.getAtivo());
        planeamentoRepository.save(plano);
    }

    // --- MÉTODOS DE MAPEAMENTO ---

    private void mapearDtoParaEntidade(MovimentoPlaneadoDTO dto, MovimentoPlaneado plano, Long utilizadorId) {
        plano.setDescricao(dto.getDescricao());
        plano.setTipo(dto.getTipo());
        plano.setFrequencia(dto.getFrequencia());
        plano.setValorBase(dto.getValorBase());
        plano.setTaxaIva(dto.getTaxaIva());
        plano.setDataInicio(dto.getDataInicio());
        plano.setDataFim(dto.getDataFim());
        plano.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // Relacionamentos Obrigatórios
        CentroCusto cc = centroCustoRepository.findById(dto.getCentroCustoId())
                .filter(c -> c.getUtilizador().getId().equals(utilizadorId))
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo inválido."));
        plano.setCentroCusto(cc);

        SeccaoHomo sh = seccaoHomoRepository.findById(dto.getSeccaoHomoId())
                .filter(s -> s.getUtilizador().getId().equals(utilizadorId))
                .orElseThrow(() -> new EntityNotFoundException("Secção Homogénea inválida."));
        plano.setSeccaoHomo(sh);

        // Relacionamentos Opcionais
        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(dto.getClienteId())
                    .filter(c -> c.getUtilizador().getId().equals(utilizadorId))
                    .orElseThrow(() -> new EntityNotFoundException("Cliente inválido."));
            plano.setCliente(cliente);
        } else {
            plano.setCliente(null);
        }

        if (dto.getFornecedorId() != null) {
            Fornecedor fornecedor = fornecedorRepository.findById(dto.getFornecedorId())
                    .filter(f -> f.getUtilizador().getId().equals(utilizadorId))
                    .orElseThrow(() -> new EntityNotFoundException("Fornecedor inválido."));
            plano.setFornecedor(fornecedor);
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
        dto.setTaxaIva(plano.getTaxaIva());
        dto.setDataInicio(plano.getDataInicio());
        dto.setDataFim(plano.getDataFim());
        dto.setAtivo(plano.getAtivo());

        dto.setCentroCustoId(plano.getCentroCusto().getId());
        dto.setSeccaoHomoId(plano.getSeccaoHomo().getId());

        if (plano.getCliente() != null) dto.setClienteId(plano.getCliente().getId());
        if (plano.getFornecedor() != null) dto.setFornecedorId(plano.getFornecedor().getId());

        return dto;
    }
}