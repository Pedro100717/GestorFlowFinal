import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Patrimonio } from '../core/models/patrimonio.model';

@Injectable({
  providedIn: 'root'
})
export class PatrimonioService {

  private readonly API_URL = 'http://localhost:8080/api/patrimonio';

  // --- O COFRE DO PATRIMÓNIO ---
  private patrimonioSubject = new BehaviorSubject<Patrimonio[]>([]);
  public patrimonio$ = this.patrimonioSubject.asObservable();

  constructor(private http: HttpClient) { }

  // Agora extrai o "dados.content" porque o Java devolve uma Page!
  carregarPatrimonioDaAPI(page: number = 0, size: number = 50): void {
    this.http.get<any>(`${this.API_URL}?page=${page}&size=${size}`).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.patrimonioSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar património:', err)
    });
  }

  // Devolve Observable<any> para não dar conflito com as Pages
  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  criarViatura(dados: any): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/viaturas`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]); 
      })
    );
  }

  criarImovel(dados: any): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/imoveis`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]);
      })
    );
  }

  criarFerramenta(dados: any): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/ferramentas`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]);
      })
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next(lista.filter(p => p.id !== id)); 
      })
    );
  }
}