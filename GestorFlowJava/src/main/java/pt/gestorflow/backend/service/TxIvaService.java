package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🚀 Adicionado
import pt.gestorflow.backend.dto.TxIvaResponseDTO;
import pt.gestorflow.backend.repository.TxIvaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j // 🚀 Anotação Mágica
@Service
@RequiredArgsConstructor
public class TxIvaService {

    private final TxIvaRepository txIvaRepository;

    @Transactional(readOnly = true) // 🚀 Otimização de RAM ativada
    public List<TxIvaResponseDTO> listarTodas() {
        log.debug("A carregar a lista global de Taxas de IVA do sistema.");

        return txIvaRepository.findAll().stream()
                .map(iva -> new TxIvaResponseDTO(iva.getId(), iva.getDescricao(), iva.getValor()))
                .collect(Collectors.toList());
    }
}