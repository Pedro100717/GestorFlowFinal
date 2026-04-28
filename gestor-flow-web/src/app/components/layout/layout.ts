import { Component, OnInit } from '@angular/core'; 
// 🚀 Não te esqueças de adicionar o NavigationEnd aqui nos imports!
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive], 
  templateUrl: './layout.html',
  styleUrl: './layout.scss'
})
export class LayoutComponent implements OnInit {

  nomeUtilizador: string = 'Utilizador';
  emailUtilizador: string = ''; 
  isMobileMenuOpen: boolean = false;

  constructor(private router: Router) {
    // 🚀 A MAGIA DE UX: Fica "à escuta" da navegação
    this.router.events.subscribe((event) => {
      // Sempre que a navegação para um ecrã novo terminar com sucesso...
      if (event instanceof NavigationEnd) {
        this.isMobileMenuOpen = false; // ...força o menu de telemóvel a fechar!
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
}