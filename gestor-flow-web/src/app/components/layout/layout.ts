import { Component, ElementRef, HostListener, OnInit } from '@angular/core'; 
import { CommonModule } from '@angular/common'; 
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
// 🚀 IMPORTAR O MOTOR DE ANIMAÇÕES DO ANGULAR
import { trigger, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive], 
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
  // 🚀 INJETAR O TRIGGER DE DESLIZE PARA OS SUBMENUS
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

  // Variáveis de controlo reativo dos submenus
  menuParamAberto: boolean = false;
  menuContasAberto: boolean = false;
  
  // 🚀 Nova variável que controla o menu do Perfil (Dropdown)
  menuPerfilAberto: boolean = false;

  constructor(private router: Router, private eRef: ElementRef) {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationEnd) {
        this.isMobileMenuOpen = false; 
      }
    });
  }

  ngOnInit() {
    const nomeCompleto = localStorage.getItem('userName'); 
    const email = localStorage.getItem('userEmail'); 
    
    if (nomeCompleto) {
      this.nomeUtilizador = nomeCompleto.split(' ')[0];
    }

    if (email) {
      this.emailUtilizador = email;
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

  // 🚀 Fecha o menu de Perfil automaticamente se o utilizador clicar fora dele
  @HostListener('document:click', ['$event'])
  cliqueFora(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.menuPerfilAberto = false;
    }
  }

  // Adiciona este método dentro da classe LayoutComponent
  fecharMenuMobile() {
    if (this.isMobileMenuOpen) {
      this.isMobileMenuOpen = false;
    }
  }
}