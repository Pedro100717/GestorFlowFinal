import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service'; // <-- Importar o serviço

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], 
  templateUrl: './login.html', 
  styleUrl: './login.scss'     
})
export class LoginComponent {

  email = '';
  senha = '';

  // Injetar o AuthService em vez do HttpClient
  constructor(private authService: AuthService, private router: Router) {}

  fazerLogin() {
    const loginData = {
      email: this.email,
      senha: this.senha
    };

    this.authService.login(loginData).subscribe({
        next: (respostaDoJava: any) => {
          localStorage.setItem('token', respostaDoJava.token);
          localStorage.setItem('userName', respostaDoJava.nome);
          localStorage.setItem('userEmail', respostaDoJava.email);
          this.router.navigate(['/app/dashboard']);
        },
        error: (erro) => {
          console.error('Erro no login:', erro);
          alert('Credenciais inválidas. Tente novamente.');
        }
    });
  }
}