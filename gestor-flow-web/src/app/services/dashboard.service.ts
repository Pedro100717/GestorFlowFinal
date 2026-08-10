import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { DashboardResumo } from '../core/models/dashboard.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly API_URL = `${environment.apiUrl}/dashboard`;

  // O Cofre do Dashboard
  private resumoSubject = new BehaviorSubject<DashboardResumo | null>(null);
  public resumo$ = this.resumoSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService 
  ) { }

  // 🚀 MODIFICADO: Avaliação independente para suportar pesquisas "abertas"
  carregarResumo(dataInicio?: string, dataFim?: string): void {
    let params = new HttpParams();
    
    // Se o frontend enviar datas, nós anexamo-las independentemente!
    if (dataInicio) {
      params = params.set('inicio', dataInicio);
    }
    if (dataFim) {
      params = params.set('fim', dataFim);
    }

    this.http.get<DashboardResumo>(`${this.API_URL}/resumo`, { params }).subscribe({
      next: (dados) => {
        this.resumoSubject.next(dados);
        // 🚀 Opcional, mas boa prática: Rastrear que o dashboard carregou bem (fica invisível em Produção)
        this.logService.debug('Resumo do dashboard carregado com sucesso.'); 
      },
      // 🚀 A CORREÇÃO: O console.error passa o testemunho ao logService
      error: (e) => this.logService.error('Erro ao carregar resumo do dashboard', e) 
    });
  }
}