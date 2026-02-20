import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  standalone: true,
  // IMPORTANTE: Adiciona RouterLink e RouterLinkActive aqui
  imports: [RouterOutlet, RouterLink, RouterLinkActive], 
  templateUrl: './layout.html',
  styleUrl: './layout.scss'
})
export class LayoutComponent {

  constructor(private router: Router) {}

  logout() {
    // 1. Limpar o token
    localStorage.removeItem('token');
    
    // 2. Redirecionar para o login
    this.router.navigate(['/login']);
  }
}