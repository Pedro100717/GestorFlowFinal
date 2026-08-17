import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const superAdminGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');

  if (token) {
    try {
      // O JWT tem 3 partes separadas por pontos. A 2ª parte é o payload (os dados).
      // Usamos o atob() nativo do browser para descodificar o base64.
      const payloadBase64 = token.split('.')[1];
      const payloadDecoded = JSON.parse(atob(payloadBase64));

      // Verifica se a role cravada no token é a do mestre
      if (payloadDecoded.role === 'SUPER_ADMIN') {
        return true; // Deixa entrar!
      }
    } catch (e) {
      console.error('Erro ao ler permissões do token', e);
    }
  }

  // Se não for SuperAdmin, pontapé de volta para o dashboard
  router.navigate(['/app/dashboard']);
  return false;
};