import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { DashboardResumo } from '../core/models/dashboard.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly API_URL = `${environment.apiUrl}/dashboard`;

  // O Cofre do Dashboard
  private resumoSubject = new BehaviorSubject<DashboardResumo | null>(null);
  public resumo$ = this.resumoSubject.asObservable();

  constructor(private http: HttpClient) { }

  // 🚀 MODIFICADO: Agora recebe as datas opcionais e envia-as no URL
  carregarResumo(dataInicio?: string, dataFim?: string): void {
    let params = new HttpParams();
    
    // Se o frontend enviar datas, nós anexamo-las (Ex: /resumo?inicio=2026-06-01&fim=2026-06-30)
    if (dataInicio && dataFim) {
      params = params.set('inicio', dataInicio).set('fim', dataFim);
    }

    this.http.get<DashboardResumo>(`${this.API_URL}/resumo`, { params }).subscribe({
      next: (dados) => this.resumoSubject.next(dados),
      error: (e) => console.error('Erro ao carregar resumo do dashboard:', e)
    });
  }
}