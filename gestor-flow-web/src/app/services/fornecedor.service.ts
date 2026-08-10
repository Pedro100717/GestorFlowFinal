import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Fornecedor } from '../core/models/fornecedor.model';
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
export class FornecedorService {

  private readonly API_URL = `${environment.apiUrl}/fornecedores`;

  // --- O NOVO COFRE DOS FORNECEDORES ---
  private fornecedoresSubject = new BehaviorSubject<Fornecedor[]>([]);
  public fornecedores$ = this.fornecedoresSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 1. Encher o Cofre (Usado pelo ecrã de Gestão de Fornecedores)
  // 🚀 TIPAGEM E PAGINAÇÃO ADICIONADAS
  carregarFornecedoresDaAPI(pagina: number = 0, tamanho: number = 1000): void {
    this.http.get<Page<Fornecedor>>(`${this.API_URL}?page=${pagina}&size=${tamanho}`).subscribe({
      next: (dados) => {
        // Como o contrato agora é Page<Fornecedor>, sabemos que 'content' existe sempre!
        const lista = dados.content || [];
        this.fornecedoresSubject.next(lista);
        this.logService.debug('Fornecedores carregados para a memória com sucesso.', lista.length);
      },
      error: (e) => this.logService.error('Erro ao carregar fornecedores:', e)
    });
  }

  // 2. Listar Clássico (Mantido para não partir a dropdown das Compras!)
  // 🚀 TIPAGEM CORRIGIDA: Devolve a Page de Fornecedores
  listar(pagina: number = 0, tamanho: number = 100): Observable<Page<Fornecedor>> {
    return this.http.get<Page<Fornecedor>>(`${this.API_URL}?page=${pagina}&size=${tamanho}`);
  }

  // 🚀 2.1 ADICIONADO: A Lupa (buscarPorId)
  buscarPorId(id: number): Observable<Fornecedor> {
    return this.http.get<Fornecedor>(`${this.API_URL}/${id}`);
  }

  // 3. Criar (Atualiza a memória na hora)
  // 🚀 Partial<Fornecedor> para permitir criar sem passar o ID
  criar(fornecedor: Partial<Fornecedor>): Observable<Fornecedor> {
    return this.http.post<Fornecedor>(this.API_URL, fornecedor).pipe(
      tap((novoFornecedor) => {
        const lista = this.fornecedoresSubject.getValue();
        this.fornecedoresSubject.next([novoFornecedor, ...lista]);
      })
    );
  }

  // 4. Atualizar (Atualiza a memória na hora)
  atualizar(id: number, fornecedor: Partial<Fornecedor>): Observable<Fornecedor> {
    return this.http.put<Fornecedor>(`${this.API_URL}/${id}`, fornecedor).pipe(
      tap((atualizado) => {
        const lista = this.fornecedoresSubject.getValue();
        this.fornecedoresSubject.next(lista.map(f => f.id === id ? atualizado : f));
      })
    );
  }

  // 5. Apagar (Atualiza a memória na hora)
  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.fornecedoresSubject.getValue();
        this.fornecedoresSubject.next(lista.filter(f => f.id !== id));
      })
    );
  }
}