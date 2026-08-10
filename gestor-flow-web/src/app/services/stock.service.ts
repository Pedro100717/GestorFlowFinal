import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 OBRIGATÓRIO: HttpParams
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { MovimentoStock } from '../core/models/stock.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

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

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 🚀 CORRIGIDO: Injeção real de parâmetros de paginação
  carregarHistoricoDaAPI(pagina: number = 0, tamanho: number = 100): void {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/historico`, { params }).subscribe({
      next: (dados) => {
        this.historicoSubject.next(dados.content);
        this.logService.debug('Histórico de acertos carregado com sucesso.', dados.content.length);
      },
      error: (err) => this.logService.error('Erro ao carregar histórico:', err)
    });
  }

  // 🚀 CORRIGIDO: Paginação aplicada na listagem direta
  listarHistorico(pagina: number = 0, tamanho: number = 100): Observable<PaginaSpring<MovimentoStock>> {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    return this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/historico`, { params });
  }

  // 🚀 ADICIONADO: Método de auditoria a um registo unitário
  buscarPorId(id: number): Observable<MovimentoStock> {
    return this.http.get<MovimentoStock>(`${this.API_URL}/historico/${id}`);
  }

  // 🚀 CORRIGIDO: Partial para aceitar apenas os dados preenchidos no form
  registarAcerto(acerto: Partial<MovimentoStock>): Observable<MovimentoStock> {
    return this.http.post<MovimentoStock>(`${this.API_URL}/acerto`, acerto).pipe(
      tap((novoAcerto) => {
        const historicoAtual = this.historicoSubject.getValue();
        this.historicoSubject.next([novoAcerto, ...historicoAtual]);
      })
    );
  }

  // 🚀 CORRIGIDO: Substituição da colagem de strings suja por HttpParams seguro
  obterHistoricoDoArtigo(artigoId: number, pagina: number = 0, tamanho: number = 20): Observable<PaginaSpring<MovimentoStock>> {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    return this.http.get<PaginaSpring<MovimentoStock>>(`${this.API_URL}/artigo/${artigoId}`, { params });
  }
}