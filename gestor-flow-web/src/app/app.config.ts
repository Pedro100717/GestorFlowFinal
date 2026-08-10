import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
// 🚀 1. Importar o motor nativo de animações
import { provideAnimations } from '@angular/platform-browser/animations';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor'; // 🚀 NOVO IMPORT

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    
    // 🚀 2. Ativar as animações globalmente na aplicação
    provideAnimations(),
    
    // AQUI É A CONFIGURAÇÃO GLOBAL DE HTTP
    provideHttpClient(
      withFetch(), // Usa a API moderna do browser
      
      // 🚀 3. ENCADEAR OS INTERCEPTORS: Primeiro a Autenticação, depois o Tratamento de Erros
      withInterceptors([authInterceptor, errorInterceptor]) 
    )
  ]
};