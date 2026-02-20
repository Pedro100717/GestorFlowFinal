import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'; // Importar isto

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor'; // O caminho correto

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    
    // AQUI É A CONFIGURAÇÃO GLOBAL DE HTTP
    provideHttpClient(
      withFetch(), // Usa a API moderna do browser
      withInterceptors([authInterceptor]) // Ativa o nosso "Segurança"
    )
  ]
};