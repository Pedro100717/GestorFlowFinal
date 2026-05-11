package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.gestorflow.backend.dto.TxIvaResponseDTO;
import pt.gestorflow.backend.repository.TxIvaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TxIvaService {

    private final TxIvaRepository txIvaRepository;

    public List<TxIvaResponseDTO> listarTodas() {
        return txIvaRepository.findAll().stream()
                .map(iva -> new TxIvaResponseDTO(iva.getId(), iva.getDescricao(), iva.getValor()))
                .collect(Collectors.toList());
    }
}