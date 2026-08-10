import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORTANTE: HttpParams
import { Observable, BehaviorSubject, tap } from 'rxjs'; // 🗑️ shareReplay apagado (era do IVA)
import { Artigo } from '../core/models/artigo.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service'; // 🚀 O NOVO INSPETOR INJETADO

// 🚀 O CONTRATO ESTANDARDIZADO
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
export class ArtigoService {

  private readonly API_URL = `${environment.apiUrl}/artigos`;
  // 🗑️ Tudo o que era IVA foi apagado!

  // --- O NOVO COFRE DOS ARTIGOS ---
  private artigosSubject = new BehaviorSubject<Artigo[]>([]);
  public artigos$ = this.artigosSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService // 🚀 LOG SERVICE INJETADO
  ) { }

  // 1. Encher o Cofre
  carregarArtigosDaAPI(pagina: number = 0, tamanho: number = 1000): void {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<Page<Artigo>>(this.API_URL, { params }).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.artigosSubject.next(lista);
        this.logService.debug('Artigos carregados para a memória com sucesso.', lista.length); // 🚀 LOG SILENCIOSO
      },
      error: (err) => this.logService.error('Erro ao carregar artigos da API', err) // 🚀 ADEUS CONSOLE.ERROR
    });
  }

  // 2. Listar Clássico
  listar(pagina: number = 0, tamanho: number = 1000): Observable<Page<Artigo>> {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    return this.http.get<Page<Artigo>>(this.API_URL, { params });
  }

  // 2.1 A Lupa
  buscarPorId(id: number): Observable<Artigo> {
    return this.http.get<Artigo>(`${this.API_URL}/${id}`);
  }

  // 3. Criar
  criar(artigo: Partial<Artigo>): Observable<Artigo> {
    return this.http.post<Artigo>(this.API_URL, artigo).pipe(
      tap((novoArtigo) => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next([novoArtigo, ...lista]);
        this.logService.info(`Novo artigo registado: ${novoArtigo.nome}`); // 🚀 RASTREABILIDADE
      })
    );
  }

  // 4. Atualizar
  atualizar(id: number, artigo: Partial<Artigo>): Observable<Artigo> {
    return this.http.put<Artigo>(`${this.API_URL}/${id}`, artigo).pipe(
      tap((artigoAtualizado) => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next(lista.map(a => a.id === id ? artigoAtualizado : a));
        this.logService.debug(`Artigo atualizado: ${id}`);
      })
    );
  }

  // 5. Apagar
  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next(lista.filter(a => a.id !== id));
        this.logService.warn(`Artigo eliminado permanentemente: ${id}`);
      })
    );
  }

  // 6. Atualização estática local
  atualizarStockNaMemoria(artigoId: number, novoStock: number): void {
    const lista = this.artigosSubject.getValue();
    this.artigosSubject.next(lista.map(a => 
      a.id === artigoId ? { ...a, stockAtual: novoStock } : a
    ));
    this.logService.debug(`Acerto de stock na memória. Artigo ${artigoId} passou para ${novoStock}`);
  }
}