package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- CRÍTICO
import pt.gestorflow.backend.dto.CentroCustoDTO;
import pt.gestorflow.backend.dto.CentroCustoResponseDTO;
import pt.gestorflow.backend.model.CentroCusto;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.CentroCustoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroCustoService {

    private final CentroCustoRepository repository;

    private Utilizador getUtilizadorLogado() {
        return (Utilizador) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 🛡️ ADICIONADO: @Transactional para garantir integridade na gravação
    @Transactional
    public CentroCustoResponseDTO criar(CentroCustoDTO dto) {
        CentroCusto cc = new CentroCusto();
        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());
        cc.setUtilizador(getUtilizadorLogado());
        return converterParaDTO(repository.save(cc));
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar() {
        return repository.findAllByUtilizadorId(getUtilizadorLogado().getId())
                .stream().map(this::converterParaDTO).toList();
    }

    // 🛡️ CORREÇÃO: @Transactional adicionado e IDOR eliminado com query segura
    @Transactional
    public CentroCustoResponseDTO atualizar(Long id, CentroCustoDTO dto) {
        Utilizador user = getUtilizadorLogado();

        CentroCusto cc = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        cc.setNome(dto.getNome());
        cc.setCodigo(dto.getCodigo());

        return converterParaDTO(repository.save(cc));
    }

    // 🛡️ ADICIONADO: Método para buscar 1 Centro de Custo específico (usado na edição do Angular)
    @Transactional(readOnly = true)
    public CentroCustoResponseDTO buscarPorId(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR: Impede que espreitem centros de custo de outros utilizadores
        CentroCusto cc = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        return converterParaDTO(cc);
    }

    // 🛡️ ADICIONADO: Método para eliminar de forma segura
    @Transactional
    public void eliminar(Long id) {
        Utilizador user = getUtilizadorLogado();

        // 🛡️ PROTEÇÃO IDOR: Garante que só o dono apaga o seu centro de custo
        CentroCusto cc = repository.findByIdAndUtilizadorId(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Centro de Custo não encontrado ou acesso negado."));

        repository.delete(cc);
    }

    private CentroCustoResponseDTO converterParaDTO(CentroCusto cc) {
        CentroCustoResponseDTO dto = new CentroCustoResponseDTO();
        dto.setId(cc.getId());
        dto.setNome(cc.getNome());
        dto.setCodigo(cc.getCodigo());
        return dto;
    }
}