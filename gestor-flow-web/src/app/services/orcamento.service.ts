import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORTADO: HttpParams
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Orcamento } from '../core/models/orcamento.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

// 🚀 ADICIONADO: O contrato padrão para a paginação do Spring
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class OrcamentoService {

  private readonly API_URL = `${environment.apiUrl}/orcamentos`;

  private orcamentosSubject = new BehaviorSubject<Orcamento[]>([]);
  public orcamentos$ = this.orcamentosSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 🚀 TIPAGEM E PAGINAÇÃO CORRIGIDAS
  carregarOrcamentosDaAPI(pagina: number = 0, tamanho: number = 100): void {
    this.http.get<Page<Orcamento>>(`${this.API_URL}?page=${pagina}&size=${tamanho}`).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.orcamentosSubject.next(lista);
        this.logService.debug('Orçamentos carregados para a memória com sucesso.', lista.length)
      },
      error: (err) => this.logService.error('Erro ao carregar orçamentos:', err)
    });
  }

  buscarPorId(id: number): Observable<Orcamento> {
    return this.http.get<Orcamento>(`${this.API_URL}/${id}`);
  }

  // 🚀 Partial<Orcamento> porque não temos ID nem Número de Documento no início
  criar(orcamento: Partial<Orcamento>): Observable<Orcamento> {
    return this.http.post<Orcamento>(this.API_URL, orcamento).pipe(
      tap((novoOrcamento) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next([novoOrcamento, ...lista]);
      })
    );
  }

  atualizar(id: number, orcamento: Partial<Orcamento>): Observable<Orcamento> {
    return this.http.put<Orcamento>(`${this.API_URL}/${id}`, orcamento).pipe(
      tap((orcAtualizado) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.map(o => o.id === id ? orcAtualizado : o));
      })
    );
  }

  // 🚀 REESCRITO: Uso de HttpParams para segurança dos URLs
  alterarEstado(id: number, estado: string): Observable<Orcamento> {
    const params = new HttpParams().set('estado', estado);

    return this.http.patch<Orcamento>(`${this.API_URL}/${id}/estado`, {}, { params }).pipe(
      tap((orcAtualizado) => {
        const lista = this.orcamentosSubject.getValue();
        this.orcamentosSubject.next(lista.map(o => o.id === id ? orcAtualizado : o));
      })
    );
  }

  // 🚀 REESCRITO: Uso de HttpParams
  converterEmVenda(id: number, contaBancariaId: number): Observable<void> {
    const params = new HttpParams().set('contaBancariaId', contaBancariaId.toString());

    return this.http.post<void>(`${this.API_URL}/${id}/converter`, {}, { params }).pipe(
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

  abrirPdfOrcamento(id: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/reports/orcamento/pdf/${id}`, { responseType: 'blob' });
  }
}