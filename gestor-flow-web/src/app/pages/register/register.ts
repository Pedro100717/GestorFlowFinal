import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router'; 
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { LogService } from '../../core/services/log.service';
import Swal from 'sweetalert2';

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

  erros: Record<string, string> = {}; 
  erroGeral: string = '';

  constructor(
    private authService: AuthService, 
    private router: Router,
    private logService: LogService
  ) {}

  fazerRegisto() {
    this.erros = {};
    this.erroGeral = '';

    // 🚀 CORREÇÃO: Enviamos 'nomeUtilizador' para coincidir com o DTO do backend
    const novoUtilizador = {
      nomeUtilizador: this.nome,
      email: this.email,
      senha: this.senha
    };

    this.authService.registar(novoUtilizador).subscribe({
        next: () => {
          this.logService.info(`Nova conta registada com sucesso: ${this.email}`); 
          
          Swal.fire({ 
            toast: true, 
            position: 'top-end', 
            icon: 'success', 
            title: 'Conta criada com sucesso! Podes fazer login agora.', 
            timer: 4000, 
            showConfirmButton: false 
          });
          this.router.navigate(['/login']);
        },
        error: (erroHttp: HttpErrorResponse) => {
          this.logService.warn(`Falha na tentativa de registo para o email: ${this.email}`, erroHttp);
          
          if (erroHttp.status === 400) {
            if (typeof erroHttp.error === 'object' && erroHttp.error !== null) {
              this.erros = erroHttp.error;
              
              let msgAlerta = '<ul style="text-align: left; margin-bottom: 0;">';
              for (const campo in erroHttp.error) {
                msgAlerta += `<li><b>${campo}:</b> ${erroHttp.error[campo]}</li>`;
              }
              msgAlerta += '</ul>';

              Swal.fire({
                icon: 'warning',
                title: 'Verifique os dados',
                html: msgAlerta,
                confirmButtonColor: '#0d6efd'
              });
            } else if (typeof erroHttp.error === 'string') {
              this.erroGeral = erroHttp.error;
              Swal.fire({
                icon: 'warning',
                title: 'Atenção',
                text: this.erroGeral,
                confirmButtonColor: '#0d6efd'
              });
            }
          } else {
            Swal.fire({
              icon: 'error',
              title: 'Erro de Servidor',
              text: 'Ocorreu um erro inesperado. Tente mais tarde.',
              confirmButtonColor: '#0d6efd'
            });
          }
        }
    });
  }
}