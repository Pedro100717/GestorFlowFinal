import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import Swal from 'sweetalert2';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      
      // Analisamos o código de estado HTTP (Status Code) que o Spring Boot nos devolveu
      switch (error.status) {
        
        case 0:
          // O backend está desligado ou o utilizador ficou sem internet
          Swal.fire({
            icon: 'error',
            title: 'Falha de Comunicação',
            text: 'Não foi possível contactar o servidor. Verifique a sua ligação à internet.',
            confirmButtonColor: '#dc3545'
          });
          break;

        case 401:
          // Sessão expirada ou tentativa de acesso sem login
          Swal.fire({
            icon: 'warning',
            title: 'Sessão Expirada',
            text: 'A sua sessão terminou por inatividade. Por favor, inicie sessão novamente.',
            confirmButtonColor: '#0d6efd'
          });
          // Limpamos o lixo local e atiramos o utilizador para a porta da rua
          localStorage.clear();
          router.navigate(['/login']);
          break;

        case 403:
          // Tem login, mas tentou mexer onde não deve (ex: um funcionário tentar apagar faturas)
          Swal.fire({
            icon: 'error',
            title: 'Acesso Negado',
            text: 'Não tem permissões suficientes para realizar esta ação.',
            confirmButtonColor: '#dc3545'
          });
          break;

        case 500:
        case 502:
        case 503:
        case 504:
          // A base de dados estourou ou o Java atirou uma NullPointerException
          Swal.fire({
            icon: 'error',
            title: 'Erro de Sistema',
            text: 'Ocorreu um erro interno no processamento do seu pedido. A equipa técnica foi notificada.',
            confirmButtonColor: '#dc3545'
          });
          break;
      }

      // 🚀 IMPORTANTE: Devolvemos o erro na mesma para que os teus componentes (ex: ComprasComponent)
      // possam fazer catch de erros específicos (como falhas de validação num formulário: 400 Bad Request)
      return throwError(() => error);
    })
  );
};