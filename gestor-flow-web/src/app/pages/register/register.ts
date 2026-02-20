import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; // RouterLink para o botão de voltar
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], 
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class RegisterComponent {

  nome = '';
  email = '';
  senha = '';

  constructor(private http: HttpClient, private router: Router) {}

  fazerRegisto() {
    // Objeto igual ao RegisterDTO do Java
    const novoUtilizador = {
      nomeUtilizador: this.nome,
      email: this.email,
      senha: this.senha
    };

    console.log('A enviar registo...', novoUtilizador);

    this.http.post('http://localhost:8080/api/auth/register', novoUtilizador)
      .subscribe({
        next: (resposta) => {
          alert('Conta criada com sucesso! Podes fazer login agora.');
          // Redireciona para o Login para a pessoa entrar
          this.router.navigate(['/login']);
        },
        error: (erro) => {
          console.error(erro);
          alert('Erro ao criar conta. Verifica se o email já existe.');
        }
      });
  }
}