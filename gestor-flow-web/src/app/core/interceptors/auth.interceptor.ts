import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  // 1. Clonar o pedido para adicionar o cabeçalho (se o token existir)
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // 2. Enviar o pedido e "escutar" se volta com erro
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      
      // Se o erro for 401 (Não autorizado) ou 403 (Proibido)
      if (error.status === 401 || error.status === 403) {
        console.warn('Sessão expirada ou inválida. A redirecionar para login...');
        
        // Limpar o lixo do storage
        localStorage.removeItem('token');
        
        // Forçar a ida para o login
        router.navigate(['/login']);
      }

      // Passar o erro para a frente (para o componente saber que falhou, se quiser mostrar um alerta)
      return throwError(() => error);
    })
  );
};
