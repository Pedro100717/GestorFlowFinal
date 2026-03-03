import { Component, OnInit } from '@angular/core'; 
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive], 
  templateUrl: './layout.html',
  styleUrl: './layout.scss'
})
export class LayoutComponent implements OnInit {

  nomeUtilizador: string = 'Utilizador';
  emailUtilizador: string = ''; // <--- Adicionada a variável do email

  constructor(private router: Router) {}

  ngOnInit() {
    // 1. Vai buscar os dados que foram guardados no momento do Login
    const nomeCompleto = localStorage.getItem('userName'); 
    const email = localStorage.getItem('userEmail'); // <--- Vai buscar o email
    
    if (nomeCompleto) {
      // 2. Corta o nome pelos espaços e fica só com a primeira palavra
      this.nomeUtilizador = nomeCompleto.split(' ')[0];
    }

    if (email) {
      // 3. Guarda o email para mostrar no HTML
      this.emailUtilizador = email;
    }
  }

  logout() {
    // 1. Limpar os dados da sessão todos
    localStorage.removeItem('token');
    localStorage.removeItem('userName'); 
    localStorage.removeItem('userEmail'); // <--- Limpa também o email
    
    // 2. Redirecionar para o login
    this.router.navigate(['/login']);
  }
}