import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

@Injectable({
  providedIn: 'root'
})
export class LogService {
  
  // 🚀 Opcional: Se quiseres mais tarde criar um endpoint no teu Spring Boot só para receber erros do Frontend
  private readonly LOG_API_URL = `${environment.apiUrl}/logs/frontend`;

  constructor(private http: HttpClient) {}

  debug(mensagem: string, payload?: any) { this.escrever(LogLevel.DEBUG, mensagem, payload); }
  info(mensagem: string, payload?: any) { this.escrever(LogLevel.INFO, mensagem, payload); }
  warn(mensagem: string, payload?: any) { this.escrever(LogLevel.WARN, mensagem, payload); }
  
  error(mensagem: string, erroOriginal?: any) {
    this.escrever(LogLevel.ERROR, mensagem, erroOriginal);
    this.enviarParaOBackendSilenciosamente(mensagem, erroOriginal);
  }

  private escrever(nivel: LogLevel, mensagem: string, payload?: any) {
    // 🛡️ REGRA DE OURO: Se estivermos em Produção, NÃO ESCREVER NADA na consola do cliente!
    // Apenas permitimos mensagens de erro graves passarem para o envio do backend.
    if (environment.production && nivel !== LogLevel.ERROR) {
      return; 
    }

    // Se estivermos em Desenvolvimento, damos cor à consola para facilitar o debug
    if (!environment.production) {
      const dataHora = new Date().toISOString();
      switch (nivel) {
        case LogLevel.DEBUG:
          console.debug(`%c[DEBUG] ${dataHora} - ${mensagem}`, 'color: #6c757d', payload || '');
          break;
        case LogLevel.INFO:
          console.info(`%c[INFO] ${dataHora} - ${mensagem}`, 'color: #0d6efd', payload || '');
          break;
        case LogLevel.WARN:
          console.warn(`%c[WARN] ${dataHora} - ${mensagem}`, 'color: #ffc107', payload || '');
          break;
        case LogLevel.ERROR:
          console.error(`%c[ERROR] ${dataHora} - ${mensagem}`, 'color: #dc3545; font-weight: bold', payload || '');
          break;
      }
    }
  }

  private enviarParaOBackendSilenciosamente(mensagem: string, erroOriginal?: any) {
    // Só enviamos para o Java se estivermos em Produção, para não encher a BD com testes locais
    if (environment.production) {
      const payloadErro = {
        mensagem: mensagem,
        detalhe: erroOriginal?.message || JSON.stringify(erroOriginal),
        url: window.location.href,
        data: new Date().toISOString()
      };

      // Fica a dica: Tens de criar este endpoint no Spring Boot mais tarde se quiseres gravar na BD.
      // O subscribe vazio é para não encravar o frontend, é um envio "fire and forget".
      this.http.post(this.LOG_API_URL, payloadErro).subscribe({
        next: () => {},
        error: () => {} // Se falhar a enviar o erro, não fazemos nada para não criar loops infinitos
      });
    }
  }
}