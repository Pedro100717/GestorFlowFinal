import { Component, ElementRef, HostListener, OnInit } from '@angular/core'; 
import { CommonModule } from '@angular/common'; 
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { trigger, style, transition, animate } from '@angular/animations';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import Swal from 'sweetalert2'; 

import { SuporteService } from '../../services/suporte.service';
import { BugReportDTO } from '../../core/models/suporte.model'; 

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, FormsModule], 
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  animations: [
    trigger('animarSubmenu', [
      transition(':enter', [
        style({ height: '0', opacity: 0, overflow: 'hidden' }),
        animate('250ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        style({ height: '*', opacity: 1, overflow: 'hidden' }),
        animate('200ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ height: '0', opacity: 0 }))
      ])
    ])
  ]
})
export class LayoutComponent implements OnInit {

  nomeUtilizador: string = 'Utilizador';
  emailUtilizador: string = ''; 
  isMobileMenuOpen: boolean = false;

  menuParamAberto: boolean = false;
  menuContasAberto: boolean = false;
  menuPerfilAberto: boolean = false;

  // 🚀 VARIÁVEL QUE CONTROLA A VISIBILIDADE DO BOTÃO DE BACKOFFICE
  isSuperAdmin: boolean = false;

  // ==========================================
  // VARIÁVEIS DO MODAL DE SUPORTE
  // ==========================================
  modalSuporteAberto: boolean = false;
  aEnviar: boolean = false;
  ticket: BugReportDTO = {
    tipo: 'BUG',
    descricao: '',
    paginaOrigem: ''
  };

  // 🚀 LISTA DE MÓDULOS AMIGÁVEIS PARA O DROPDOWN
  modulosGestorFlow = [
    'Dashboard Principal',
    'Gestão de Artigos',
    'Gestão de Clientes',
    'Gestão de Fornecedores',
    'Gestão de Compras',
    'Gestão de Vendas',
    'Tesouraria',
    'Património',
    'Acertos de Stock',
    'Orçamentos',
    'Gestão de Tarefas',
    'Contas Correntes',
    'Contabilidade Analítica',
    'Definições da Conta',
    'Outro / Não aplicável'
  ];

  constructor(
    private router: Router, 
    private eRef: ElementRef,
    private suporteService: SuporteService
  ) {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.isMobileMenuOpen = false; 
      }
    });
  }

  ngOnInit() {
    const nomeCompleto = localStorage.getItem('userName'); 
    const email = localStorage.getItem('userEmail'); 
    const token = localStorage.getItem('token'); 
    
    if (nomeCompleto) {
      this.nomeUtilizador = nomeCompleto.split(' ')[0];
    }

    if (email) {
      this.emailUtilizador = email;
      this.ticket.emailUtilizador = email; 
    }

    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.isSuperAdmin = (payload.role === 'SUPER_ADMIN');
      } catch (e) {
        console.error('Erro a descodificar o token:', e);
        this.isSuperAdmin = false;
      }
    }
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('userName'); 
    localStorage.removeItem('userEmail'); 
    this.router.navigate(['/login']);
  }

  toggleMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  @HostListener('document:click', ['$event'])
  cliqueFora(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.menuPerfilAberto = false;
    }
  }

  fecharMenuMobile() {
    if (this.isMobileMenuOpen) {
      this.isMobileMenuOpen = false;
    }
  }

  // ==========================================
  // LÓGICA DO SUPORTE (Com SweetAlert2)
  // ==========================================
  abrirModalSuporte() {
    this.ticket.descricao = '';
    this.ticket.tipo = 'BUG';
    // 🚀 A MÁGICA ACONTECE AQUI: Preenche com o nome bonito consoante a rota!
    this.ticket.paginaOrigem = this.descobrirModuloPeloUrl(this.router.url);
    this.modalSuporteAberto = true;
  }

  // 🚀 O CÉREBRO QUE TRADUZ URLs EM NOMES AMIGÁVEIS
  private descobrirModuloPeloUrl(url: string): string {
    const urlLower = url.toLowerCase();
    if (urlLower.includes('artigos')) return 'Gestão de Artigos';
    if (urlLower.includes('clientes') && !urlLower.includes('contas')) return 'Gestão de Clientes';
    if (urlLower.includes('fornecedores') && !urlLower.includes('contas')) return 'Gestão de Fornecedores';
    if (urlLower.includes('compras')) return 'Gestão de Compras';
    if (urlLower.includes('vendas')) return 'Gestão de Vendas';
    if (urlLower.includes('tesouraria')) return 'Tesouraria';
    if (urlLower.includes('patrimonio')) return 'Património';
    if (urlLower.includes('stock')) return 'Acertos de Stock';
    if (urlLower.includes('orcamentos')) return 'Orçamentos';
    if (urlLower.includes('tarefas')) return 'Gestão de Tarefas';
    if (urlLower.includes('contas-correntes')) return 'Contas Correntes';
    if (urlLower.includes('analitica')) return 'Contabilidade Analítica';
    if (urlLower.includes('definicoes')) return 'Definições da Conta';
    if (urlLower.includes('dashboard')) return 'Dashboard Principal';
    
    return 'Outro / Não aplicável';
  }

  fecharModalSuporte() {
    this.modalSuporteAberto = false;
  }

  enviarTicket() {
    if (!this.ticket.descricao.trim()) {
      Swal.fire({
        title: 'Atenção',
        text: 'Por favor, descreve o problema ou sugestão antes de enviar.',
        icon: 'warning',
        confirmButtonColor: '#f59e0b'
      });
      return;
    }

    this.aEnviar = true;
    this.suporteService.submeterTicket(this.ticket).subscribe({
      next: (resposta: string) => {
        this.aEnviar = false;
        this.fecharModalSuporte();
        
        Swal.fire({
          title: 'Enviado!',
          text: 'A tua mensagem foi enviada com sucesso. A equipa GestorFlow vai analisar em breve.',
          icon: 'success',
          confirmButtonColor: '#10b981'
        });
      },
      error: (err: HttpErrorResponse) => {
        this.aEnviar = false;
        
        Swal.fire({
          title: 'Oops!',
          text: 'Ocorreu um erro ao enviar o pedido. Tenta novamente mais tarde.',
          icon: 'error',
          confirmButtonColor: '#ef4444'
        });
        console.error('Falha no pedido HTTP de Suporte:', err.message);
      }
    });
  }
}