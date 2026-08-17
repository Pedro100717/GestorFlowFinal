package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.gestorflow.backend.dto.BugReportDTO;
import pt.gestorflow.backend.dto.TicketResponseDTO; // 🚀 O NOVO DTO
import pt.gestorflow.backend.model.ReportSuporte;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.ReportSuporteRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuporteService {

    private final ReportSuporteRepository suporteRepository;
    private final UtilizadorRepository utilizadorRepository;

    @Transactional
    public void processarNovoTicket(BugReportDTO dto) {
        ReportSuporte ticket = new ReportSuporte();
        ticket.setTipo(dto.getTipo());
        ticket.setDescricao(dto.getDescricao());
        ticket.setPaginaOrigem(dto.getPaginaOrigem());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Long idDoUtilizador) {
                Utilizador u = utilizadorRepository.findById(idDoUtilizador).orElse(null);
                if (u != null) {
                    ticket.setUtilizador(u);
                    ticket.setEmailUtilizador(u.getEmail());
                }
            }
        } else {
            ticket.setEmailUtilizador(dto.getEmailUtilizador());
        }

        suporteRepository.save(ticket);
        log.info("Ticket de suporte guardado com sucesso na base de dados. Tipo: {}", ticket.getTipo());
    }

    // 🚀 NOVO MÉTODO PARA O BACKOFFICE (Devolve o DTO seguro em vez da Entidade)
    public List<TicketResponseDTO> listarTodosTickets() {
        return suporteRepository.findAll().stream().map(ticket -> {
            TicketResponseDTO dto = new TicketResponseDTO();
            dto.setId(ticket.getId());
            dto.setTipo(ticket.getTipo());
            dto.setDescricao(ticket.getDescricao());
            dto.setPaginaOrigem(ticket.getPaginaOrigem());

            if (ticket.getUtilizador() != null) {
                dto.setNomeUtilizador(ticket.getUtilizador().getNomeUtilizador());
                dto.setEmailUtilizador(ticket.getUtilizador().getEmail());
            } else {
                dto.setNomeUtilizador("Anónimo");
                dto.setEmailUtilizador(ticket.getEmailUtilizador());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void apagarTicket(Long id) {
        suporteRepository.deleteById(id);
        log.info("Ticket de suporte com ID {} apagado com sucesso.", id);
    }
}