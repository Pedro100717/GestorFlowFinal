import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SuporteService } from '../../../services/suporte.service'; 
import { ReportSuporte } from '../../../core/models/suporte.model';     
import Swal from 'sweetalert2'; // 🚀 IMPORTAR O SWEETALERT

@Component({
  selector: 'app-admin-suporte',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-suporte.html'
})
export class AdminSuporteComponent implements OnInit {

  tickets: ReportSuporte[] = [];
  aCarregar: boolean = true;
  erro: string = '';

  constructor(private suporteService: SuporteService) {}

  ngOnInit(): void {
    this.carregarTickets();
  }

  carregarTickets() {
    this.aCarregar = true;
    this.erro = '';

    this.suporteService.listarTickets().subscribe({
      next: (dados: ReportSuporte[]) => {
        this.tickets = dados;
        this.aCarregar = false;
      },
      error: (err) => {
        this.erro = 'Acesso Negado ou Erro de Servidor. Confirma se tens permissão de Super Admin.';
        this.aCarregar = false;
        console.error(err);
      }
    });
  }

  // 🚀 NOVO MÉTODO PARA LER O TICKET COMPLETO
  verDetalhes(ticket: ReportSuporte) {
    Swal.fire({
      title: `<span class="fs-5">Ticket #${ticket.id} - ${ticket.tipo}</span>`,
      html: `
        <div class="text-start mt-3">
          <p class="mb-1"><strong>Utilizador:</strong> ${ticket.nomeUtilizador}</p>
          <p class="mb-3"><strong>Página de Origem:</strong> <code class="bg-light px-2 py-1 rounded">${ticket.paginaOrigem}</code></p>
          <hr>
          <p class="mt-3 fw-bold text-secondary mb-1">Descrição do Problema:</p>
          <div class="p-3 bg-light rounded border text-muted" style="white-space: pre-wrap; font-size: 0.95rem;">${ticket.descricao}</div>
        </div>
      `,
      showCloseButton: true,
      confirmButtonText: 'Fechar',
      confirmButtonColor: '#6c757d',
      width: '600px'
    });
  }

  // 🚀 MÉTODO PARA APAGAR O TICKET
  resolverTicket(id: number) {
    Swal.fire({
      title: 'Resolver Ticket?',
      text: "Isto irá apagar o ticket da base de dados permanentemente.",
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#198754', // Verde Success do Bootstrap
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sim, resolver!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        
        // Chama o serviço para apagar
        this.suporteService.apagarTicket(id).subscribe({
          next: () => {
            // Remove o ticket da lista visualmente sem ter de dar refresh à página inteira
            this.tickets = this.tickets.filter(t => t.id !== id);
            
            Swal.fire({
              toast: true,
              position: 'top-end',
              icon: 'success',
              title: 'Ticket resolvido e limpo!',
              showConfirmButton: false,
              timer: 3000
            });
          },
          error: (err) => {
            Swal.fire('Erro', 'Não foi possível resolver o ticket. Tenta de novo.', 'error');
            console.error(err);
          }
        });
      }
    });
  }
}