import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { Router, ActivatedRoute, RouterLink } from '@angular/router'; 
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO DOS ERROS
import { AuthService } from '../../services/auth.service'; 
import { LogService } from '../../core/services/log.service'; // 🚀 INJEÇÃO DO NOSSO INSPETOR
import Swal from 'sweetalert2';

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

  constructor(
    private authService: AuthService, 
    private router: Router,
    private route: ActivatedRoute,
    private logService: LogService // 🚀 SERVIÇO DECLARADO NO CONSTRUTOR
  ) {}

  fazerLogin() {
    const loginData = {
      email: this.email,
      senha: this.senha
    };

    this.authService.login(loginData).subscribe({
        next: (respostaDoJava: any) => {
          // 1. Guardar os dados da sessão
          localStorage.setItem('token', respostaDoJava.token);
          localStorage.setItem('userName', respostaDoJava.nome);
          localStorage.setItem('userEmail', respostaDoJava.email);
          
          // 🚀 AUDITORIA DE SEGURANÇA: Registar quem entrou com sucesso
          this.logService.info(`Login efetuado com sucesso para o email: ${respostaDoJava.email}`); 

          // 2. Lemos o papelinho de redirecionamento!
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/app/dashboard';
          this.router.navigateByUrl(returnUrl);
        },
        error: (erro: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          // 🚀 AUDITORIA DE SEGURANÇA: Registar silenciosamente tentativas falhadas
          // Usamos 'warn' porque uma falha de credenciais não é um erro de sistema, é um alerta de segurança
          this.logService.warn(`Tentativa de login falhada para o email: ${this.email}`, erro); 
          
          Swal.fire({
            icon: 'error',
            title: 'Erro no Login',
            text: 'Email ou senha incorretos. Por favor, tente novamente.',
            confirmButtonColor: '#212529' 
          });
        }
    });
  }
}