import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Necessário para o [(ngModel)]
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], // Importar módulos essenciais para o formulário
  templateUrl: './login.html', // Confirma se o nome do teu ficheiro é este
  styleUrl: './login.scss'     // Confirma se o nome do teu ficheiro é este
})
export class LoginComponent {

  // Variáveis ligadas ao HTML via [(ngModel)]
  email = '';
  senha = '';

  // Injeção de dependências (HTTP para ligar ao Java, Router para mudar de página)
  constructor(private http: HttpClient, private router: Router) {}

  fazerLogin() {
    console.log('A tentar login com:', this.email);

    // Objeto que o Backend Java espera receber (LoginDTO)
    const loginData = {
      email: this.email,
      senha: this.senha
    };

    // Chamada POST ao Backend
    this.http.post('http://localhost:8080/api/auth/login', loginData, { responseType: 'text' })
      .subscribe({
        next: (token) => {
          // 1. Sucesso: O Java devolveu o Token
          console.log('Login Sucesso! Token:', token);
          
          // 2. Guardar o token no navegador (para usarmos depois nas Vendas/Clientes)
          localStorage.setItem('token', token);
          
          // 3. Redirecionar para o Dashboard
          this.router.navigate(['/app/dashboard']);
        },
        error: (erro) => {
          // Erro: Senha errada ou servidor desligado
          console.error('Erro no login:', erro);
          alert('Login falhou! Verifica se o email/senha estão corretos ou se o Backend está ligado.');
        }
      });
  }
}