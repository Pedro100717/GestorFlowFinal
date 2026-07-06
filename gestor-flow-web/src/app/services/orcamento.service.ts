import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Orcamento } from '../core/models/orcamento.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OrcamentoService {

  private readonly API_URL = `${environment.apiUrl}/orcamentos`;

  private orcamentosSubject = new BehaviorSubject<Orcamento[]>([]);
  public orcamentos$ = this.orcamentosSubject.asObservable();

  constructor(private http: HttpClient) { }

  carregarOrcamentosDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.orcamentosSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar orçamentos:', err)
    });
  }

  buscarPorId(id: number): Observable<Orcamento> {
    return this.http.get<Orcamento>(`${this.API_URL}/${id}`);
  }

  criar(orcamento: Orcamento): Observable<Orcamento> {
    return this.http.post<Orcamento>(this.API_URL, orcamento).pipe(
      tap((novoOrcamento) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next([novoOrcamento, ...lista]);
      })
    );
  }

  atualizar(id: number, orcamento: Orcamento): Observable<Orcamento> {
    return this.http.put<Orcamento>(`${this.API_URL}/${id}`, orcamento).pipe(
      tap((orcAtualizado) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.map(o => o.id === id ? orcAtualizado : o));
      })
    );
  }

  alterarEstado(id: number, estado: string): Observable<Orcamento> {
    return this.http.patch<Orcamento>(`${this.API_URL}/${id}/estado?estado=${estado}`, {}).pipe(
      tap((orcAtualizado) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.map(o => o.id === id ? orcAtualizado : o));
      })
    );
  }

  converterEmVenda(id: number, contaBancariaId: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/converter?contaBancariaId=${contaBancariaId}`, {}).pipe(
      tap(() => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.map(o => {
          if (o.id === id) {
            return { ...o, estado: 'CONVERTIDO_VENDA' };
          }
          return o;
        }));
      })
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.filter(o => o.id !== id));
      })
    );
  }

  // 🚀 NOVO MÉTODO: Extração do PDF
  abrirPdfOrcamento(id: number): Observable<Blob> {
    // responseType 'blob' é fundamental para receber ficheiros em vez de JSON
    return this.http.get(`${environment.apiUrl}/reports/orcamento/pdf/${id}`, { responseType: 'blob' });
  }
}