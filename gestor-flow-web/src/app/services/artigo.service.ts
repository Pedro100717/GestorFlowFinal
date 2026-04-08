import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, shareReplay } from 'rxjs'; // <--- ADICIONADOS IMPORTS NOVOS
import { Artigo } from '../core/models/artigo.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ArtigoService {

  private readonly API_URL = `${environment.apiUrl}/artigos`;
  
  // Cache das Taxas de IVA (Mantida intacta!)
  private cacheTaxasIva$: Observable<any[]> | null = null;

  // --- O NOVO COFRE DOS ARTIGOS ---
  private artigosSubject = new BehaviorSubject<Artigo[]>([]);
  public artigos$ = this.artigosSubject.asObservable();

  constructor(private http: HttpClient) { }

  // 1. Encher o Cofre (Usado pelo ecrã de Gestão de Artigos e Inventário)
  carregarArtigosDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.artigosSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar artigos:', err)
    });
  }

  // 2. Listar Clássico (Mantido para não partir dropdowns antigas)
  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  // 3. Criar (Atualiza a memória na hora)
  criar(artigo: Artigo): Observable<Artigo> {
    return this.http.post<Artigo>(this.API_URL, artigo).pipe(
      tap((novoArtigo) => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next([novoArtigo, ...lista]);
      })
    );
  }

  // 4. Atualizar (Atualiza a memória na hora)
  atualizar(id: number, artigo: Artigo): Observable<Artigo> {
    return this.http.put<Artigo>(`${this.API_URL}/${id}`, artigo).pipe(
      tap((artigoAtualizado) => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next(lista.map(a => a.id === id ? artigoAtualizado : a));
      })
    );
  }

  // 5. Buscar Taxas de IVA (MANTIDO INTACTO!)
  listarTaxasIva(): Observable<any[]> {
    if (!this.cacheTaxasIva$) {
      this.cacheTaxasIva$ = this.http.get<any[]>(`${this.API_URL}/taxas-iva`).pipe(
        shareReplay(1)
      );
    }
    return this.cacheTaxasIva$;
  }

  // 6. Apagar (Atualiza a memória na hora)
  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.artigosSubject.getValue();
        this.artigosSubject.next(lista.filter(a => a.id !== id));
      })
    );
  }

  // Adiciona esta função no teu ArtigoService
  atualizarStockNaMemoria(artigoId: number, novoStock: number): void {
    const lista = this.artigosSubject.getValue();
    this.artigosSubject.next(lista.map(a => 
      a.id === artigoId ? { ...a, stockAtual: novoStock } : a
    ));
  }
}