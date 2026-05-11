package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.MovimentoPlaneadoDTO;
import pt.gestorflow.backend.model.*;
import pt.gestorflow.backend.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaneamentoService {

    private final MovimentoPlaneadoRepository planeamentoRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final TxIvaRepository txIvaRepository;
    private final AuthService authService;
    private final DocumentoTesourariaRepository documentoTesourariaRepository;

    // =========================================================================
    // --- 🚀 O BOTÃO MÁGICO: EFETIVAR PLANEAMENTO (SEM COMPRAS/VENDAS) ---
    // =========================================================================

    @Transactional
    public void gerarFaturaPendente(Long planoId) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        MovimentoPlaneado plano = planeamentoRepository.findByIdAndUtilizadorId(planoId, utilizadorId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado."));

        LocalDate hoje = LocalDate.now();
        if (plano.getDataUltimoProcessamento() != null && plano.getDataUltimoProcessamento().getMonth() == hoje.getMonth() && plano.getDataUltimoProcessamento().getYear() == hoje.getYear()) {
            throw new IllegalStateException("Este plano já foi processado no mês corrente.");
        }

        // 🚀 AQUI NASCE O DOCUMENTO PURO DE TESOURARIA
        DocumentoTesouraria doc = new DocumentoTesouraria();
        doc.setDescricao(plano.getDescricao());
        doc.setTipo(plano.getTipo());
        doc.setValorTotal(plano.getValorComIva()); // Usa a matemática do IVA!
        doc.setDataEmissao(LocalDateTime.now());
        doc.setUtilizador(plano.getUtilizador());
        documentoTesourariaRepository.save(doc);

        plano.setDataUltimoProcessamento(hoje);
        planeamentoRepository.save(plano);
    }
    // =========================================================================
    // --- GESTÃO CRUD SIMPLIFICADA (ESTILO EXCEL) ---
    // =========================================================================

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
    // --- MAPEAMENTOS (ZERO BUROCRACIA COMERCIAL) ---
    // =========================================================================

    private void mapearDtoParaEntidade(MovimentoPlaneadoDTO dto, MovimentoPlaneado plano, Long utilizadorId) {
        plano.setDescricao(dto.getDescricao());
        plano.setTipo(dto.getTipo());
        plano.setFrequencia(dto.getFrequencia());
        plano.setValorBase(dto.getValorBase());
        plano.setDataInicio(dto.getDataInicio());
        plano.setDataFim(dto.getDataFim());
        plano.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);

        // 🔗 Taxa de IVA: O único vínculo obrigatório para o cálculo
        if (dto.getTaxaIvaId() != null) {
            TxIva iva = txIvaRepository.findById(dto.getTaxaIvaId())
                    .orElseThrow(() -> new EntityNotFoundException("Taxa de IVA inválida."));
            plano.setTaxaIva(iva);
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

        if (plano.getTaxaIva() != null) {
            dto.setTaxaIvaId(plano.getTaxaIva().getId());
        }

        return dto;
    }
}