import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs'; 
import Swal from 'sweetalert2'; 

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  let authReq = req;
  
  // 🚀 1. BLINDAGEM DE SEGURANÇA: Só injeta o token se o destino for o NOSSO backend
  // Assumindo que a tua API tem sempre '/api/' no URL. Ajusta se usares environment.apiUrl
  const isApiUrl = req.url.includes('/api/');

  if (token && isApiUrl) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      
      // 🚀 CORREÇÃO P1-03: Lidar com a expulsão corretamente sem deixar o componente congelado
      if (error.status === 401 || error.status === 403) {
        localStorage.removeItem('token');
        
        // Evita *spam* de modais se vários pedidos falharem ao mesmo tempo
        if (router.url !== '/login' && !document.querySelector('.swal2-container')) {
          Swal.fire({
            icon: 'info',
            title: 'Sessão Expirada',
            text: 'A sua sessão terminou. Por favor, faça login novamente.',
            confirmButtonColor: '#212529',
            timer: 3000
          });
          router.navigate(['/login']);
        }
        
        // IMPORTANTE: Devolve throwError em vez de EMPTY para avisar o componente!
        return throwError(() => new Error('SESSAO_EXPIRADA')); 
      }

      // Regra do 412 
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
        return throwError(() => new Error('CONFIGURACAO_FALTA')); // Substituído EMPTY
      }

      // 🚀 3. REDE DE SEGURANÇA: Backend em baixo (0) ou Erros Internos Críticos (500)
      if (error.status === 0) {
        Swal.fire({
          icon: 'error',
          title: 'Sem Ligação',
          text: 'Não foi possível ligar ao servidor. Verifique a sua internet ou tente mais tarde.',
          confirmButtonColor: '#d33'
        });
      } else if (error.status >= 500) {
        Swal.fire({
          icon: 'error',
          title: 'Erro de Sistema',
          text: 'Ocorreu um erro interno no servidor. A nossa equipa já foi notificada.',
          confirmButtonColor: '#d33'
        });
      }

      return throwError(() => error);
    })
  );
};