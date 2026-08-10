import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Patrimonio } from '../core/models/patrimonio.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

// 🚀 ADICIONADO: O nosso contrato de paginação standard
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
export class PatrimonioService {

  private readonly API_URL = `${environment.apiUrl}/patrimonio`;

  // --- O COFRE DO PATRIMÓNIO ---
  private patrimonioSubject = new BehaviorSubject<Patrimonio[]>([]);
  public patrimonio$ = this.patrimonioSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 🚀 TIPAGEM CORRIGIDA: Adeus 'any'! Agora sabemos que recebemos uma Page de Património
  carregarPatrimonioDaAPI(page: number = 0, size: number = 50): void {
    this.http.get<Page<Patrimonio>>(`${this.API_URL}?page=${page}&size=${size}`).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.patrimonioSubject.next(lista);
        this.logService.debug('Património carregado para a memória com sucesso.', lista.length)
      },
      error: (err) => this.logService.error('Erro ao carregar património:', err)
    });
  }

  // 🚀 TIPAGEM CORRIGIDA: Devolve uma Page blindada em vez de 'any'
  listar(page: number = 0, size: number = 100): Observable<Page<Patrimonio>> {
    return this.http.get<Page<Patrimonio>>(`${this.API_URL}?page=${page}&size=${size}`);
  }

  // 🚀 ADICIONADO: A Lupa (buscarPorId)
  buscarPorId(id: number): Observable<Patrimonio> {
    return this.http.get<Patrimonio>(`${this.API_URL}/${id}`);
  }

  // 🚀 TIPAGEM CORRIGIDA: Partial<Patrimonio> em vez de 'any'
  criarViatura(dados: Partial<Patrimonio>): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/viaturas`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]); 
      })
    );
  }

  criarImovel(dados: Partial<Patrimonio>): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/imoveis`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]);
      })
    );
  }

  criarFerramenta(dados: Partial<Patrimonio>): Observable<Patrimonio> {
    return this.http.post<Patrimonio>(`${this.API_URL}/ferramentas`, dados).pipe(
      tap((novoAtivo) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next([novoAtivo, ...lista]);
      })
    );
  }

  // 🚀 ADICIONADO: O método de atualização que faltava para os ativos
  atualizar(id: number, dados: Partial<Patrimonio>): Observable<Patrimonio> {
    return this.http.put<Patrimonio>(`${this.API_URL}/${id}`, dados).pipe(
      tap((ativoAtualizado) => {
        const lista = this.patrimonioSubject.getValue();
        this.patrimonioSubject.next(lista.map(p => p.id === id ? ativoAtualizado : p));
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