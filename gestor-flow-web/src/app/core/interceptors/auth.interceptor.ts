import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, EMPTY } from 'rxjs'; 
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
      
      // 🚀 2. UX: Avisar o utilizador que foi expulso (Sessão Expirada)
      if (error.status === 401 || error.status === 403) {
        localStorage.removeItem('token');
        
        // Só mostramos o alerta se não estivermos já na página de login
        if (router.url !== '/login') {
          Swal.fire({
            icon: 'info',
            title: 'Sessão Expirada',
            text: 'Por motivos de segurança, a sua sessão terminou. Por favor, faça login novamente.',
            confirmButtonColor: '#212529',
            timer: 3000 // Fecha automaticamente após 3 segundos
          });
        }
        
        router.navigate(['/login']);
        return EMPTY; // Corta a propagação
      }

      // Regra do 412 (Impecável, mantida igual)
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
        return EMPTY; 
      }

      // 🚀 3. REDE DE SEGURANÇA: Backend em baixo (0) ou Erros Internos Críticos (500)
      if (error.status === 0) {
        Swal.fire({
          icon: 'error',
          title: 'Sem Ligação',
          text: 'Não foi possível ligar ao servidor. Verifique a sua internet ou tente mais tarde.',
          confirmButtonColor: '#d33'
        });
        // Aqui deixamos passar com throwError para que o Loading Spinner do componente saiba que tem de parar
      } else if (error.status >= 500) {
        Swal.fire({
          icon: 'error',
          title: 'Erro de Sistema',
          text: 'Ocorreu um erro interno no servidor. A nossa equipa já foi notificada.',
          confirmButtonColor: '#d33'
        });
      }

      // Passa o erro (400, 404, etc.) para o componente tratar especificamente, se quiser
      return throwError(() => error);
    })
  );
};