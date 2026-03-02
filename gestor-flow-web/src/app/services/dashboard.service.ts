import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { DashboardResumo } from '../core/models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  private readonly API_URL = 'http://localhost:8080/api/dashboard';

  // O Cofre do Dashboard
  private resumoSubject = new BehaviorSubject<DashboardResumo | null>(null);
  public resumo$ = this.resumoSubject.asObservable();

  constructor(private http: HttpClient) { }

  // Vai ao Java buscar dados e atualiza o Cofre silenciosamente
  carregarResumo(): void {
    this.http.get<DashboardResumo>(`${this.API_URL}/resumo`).subscribe({
      next: (dados) => this.resumoSubject.next(dados),
      error: (e) => console.error('Erro ao carregar resumo do dashboard:', e)
    });
  }
}