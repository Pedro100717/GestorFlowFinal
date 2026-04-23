import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { MovimentoStock } from '../core/models/stock.model';
import { environment } from '../../environments/environment';

// 🛡️ A INTERFACE INDUSTRIAL: Ensina o Angular a ler a Paginação do Spring Boot
export interface PaginaSpring<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class StockService {

  private readonly API_URL = `${environment.apiUrl}/stock`;

  // --- O COFRE DO HISTÓRICO DE ACERTOS ---
  private historicoSubject = new BehaviorSubject<MovimentoStock[]>([]);
  public historico$ = this.historicoSubject.asObservable();

  constructor(private http: HttpClient) { }

  carregarHistoricoDaAPI(): void {
    // 🛡️ Tipagem forte: Sabemos que vem uma Página cheia de Movimentos
    this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/historico`).subscribe({
      next: (dados) => {
        // Já não precisamos do "dados.content || dados" feio, o TS sabe que o content existe!
        this.historicoSubject.next(dados.content);
      },
      error: (err) => console.error('Erro ao carregar histórico:', err)
    });
  }

  listarHistorico(): Observable<PaginaSpring<MovimentoStock>> {
    return this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/historico`);
  }

  registarAcerto(acerto: MovimentoStock): Observable<MovimentoStock> {
    return this.http.post<MovimentoStock>(`${this.API_URL}/acerto`, acerto).pipe(
      tap((novoAcerto) => {
        const historicoAtual = this.historicoSubject.getValue();
        this.historicoSubject.next([novoAcerto, ...historicoAtual]);
      })
    );
  }

  obterHistoricoDoArtigo(artigoId: number, page: number = 0, size: number = 20): Observable<PaginaSpring<MovimentoStock>> {
    // 🛡️ Retorna a tipagem exata da Página
    return this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/artigo/${artigoId}?page=${page}&size=${size}`);
  }
}