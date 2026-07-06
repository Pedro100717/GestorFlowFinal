import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
// 🚀 1. IMPORTA O 'EMPTY' DO RXJS
import { catchError, throwError, EMPTY } from 'rxjs'; 
import Swal from 'sweetalert2'; 

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      
      if (error.status === 401 || error.status === 403) {
        console.warn('Sessão expirada ou inválida. A redirecionar para login...');
        localStorage.removeItem('token');
        router.navigate(['/login']);
        return EMPTY; // Corta a propagação do erro
      }

      // 🚀 REGRA DO 412
      if (error.status === 412) {
        
        Swal.fire({
          icon: 'warning',
          title: 'Configuração Necessária',
          text: 'Para emitir documentos oficiais, tem de preencher primeiro os dados da Entidade Faturadora.',
          confirmButtonText: 'Configurar Agora',
          confirmButtonColor: '#212529',
          allowOutsideClick: false
        }).then((result) => {
          if (result.isConfirmed) {
            router.navigate(['/app/definicoes']);
          }
        });

        // 🚀 2. O SEGREDO ESTÁ AQUI: Retornar EMPTY impede que o ecrã das contas correntes veja o erro e cancele o nosso alerta!
        return EMPTY; 
      }

      return throwError(() => error);
    })
  );
};