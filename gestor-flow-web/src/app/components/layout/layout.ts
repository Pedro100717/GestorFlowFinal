import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], 
  templateUrl: '../../pages/login/login.html', 
  styleUrl: '../../pages/login/login.scss'     
})
export class LoginComponent {

  email = '';
  senha = '';

  constructor(private http: HttpClient, private router: Router) {}

  fazerLogin() {
    console.log('A tentar login com:', this.email);

    const loginData = {
      email: this.email,
      senha: this.senha
    };

    // Chamada POST ao Backend (Sem o responseType: 'text', porque agora recebemos um JSON!)
    this.http.post('http://localhost:8080/api/auth/login', loginData)
      .subscribe({
        next: (respostaDoJava: any) => {
          // 1. Sucesso: O Java devolveu o pacote com os dados
          console.log('Login Sucesso! Resposta:', respostaDoJava);
          
          // 2. Guardar TUDO no navegador
          localStorage.setItem('token', respostaDoJava.token);
          localStorage.setItem('userName', respostaDoJava.nome);
          localStorage.setItem('userEmail', respostaDoJava.email);
          
          // 3. Redirecionar para o Dashboard
          this.router.navigate(['/app/dashboard']);
        },
        error: (erro) => {
          console.error('Erro no login:', erro);
          alert('Login falhou! Verifica se o email/senha estão corretos ou se o Backend está ligado.');
        }
      });
  }
}