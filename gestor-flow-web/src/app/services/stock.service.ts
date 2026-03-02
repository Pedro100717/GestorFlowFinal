import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { MovimentoStock } from '../core/models/stock.model';

@Injectable({
  providedIn: 'root'
})
export class StockService {

  private readonly API_URL = 'http://localhost:8080/api/stock';

  // --- O COFRE DO HISTÓRICO DE ACERTOS ---
  private historicoSubject = new BehaviorSubject<MovimentoStock[]>([]);
  public historico$ = this.historicoSubject.asObservable();

  constructor(private http: HttpClient) { }

  carregarHistoricoDaAPI(): void {
    this.http.get<any>(`${this.API_URL}/historico`).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.historicoSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar histórico:', err)
    });
  }

  listarHistorico(): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/historico`);
  }

  registarAcerto(acerto: MovimentoStock): Observable<MovimentoStock> {
    return this.http.post<MovimentoStock>(`${this.API_URL}/acerto`, acerto).pipe(
      tap((novoAcerto) => {
        const historicoAtual = this.historicoSubject.getValue();
        this.historicoSubject.next([novoAcerto, ...historicoAtual]);
      })
    );
  }
}