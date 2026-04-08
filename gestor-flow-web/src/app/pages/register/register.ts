import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; 
import { AuthService } from '../../services/auth.service';

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

  erros: any = {}; 
  erroGeral: string = '';

  constructor(private authService: AuthService, private router: Router) {}

  fazerRegisto() {
    this.erros = {};
    this.erroGeral = '';

    const novoUtilizador = {
      nomeUtilizador: this.nome,
      email: this.email,
      senha: this.senha
    };

    this.authService.registar(novoUtilizador).subscribe({
        next: (resposta) => {
          alert('Conta criada com sucesso! Podes fazer login agora.');
          this.router.navigate(['/login']);
        },
        error: (erroHttp) => {
          console.error('Erro do backend:', erroHttp);
          
          if (erroHttp.status === 400) {
            
            // Se o erro for o DTO com as várias mensagens de validação
            if (typeof erroHttp.error === 'object' && erroHttp.error !== null) {
              this.erros = erroHttp.error;
              
              // CRIAR O POP-UP PARA O UTILIZADOR
              let msgAlerta = "Atenção! Verifique os seguintes campos:\n\n";
              for (const campo in erroHttp.error) {
                msgAlerta += `❌ ${erroHttp.error[campo]}\n`;
              }
              alert(msgAlerta);
            } 
            // Se for um erro geral (ex: "Email já existe")
            else if (typeof erroHttp.error === 'string') {
              this.erroGeral = erroHttp.error;
              alert("❌ " + this.erroGeral);
            }
          } else {
            alert('Ocorreu um erro no servidor. Tente mais tarde.');
          }
        }
    });
  }
}