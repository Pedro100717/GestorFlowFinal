import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Cliente } from '../core/models/cliente.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

// 🚀 ADICIONADO: A mesma interface rápida para mapear a paginação do Spring Boot
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
export class ClienteService {

  private readonly API_URL = `${environment.apiUrl}/clientes`;

  // --- O NOVO COFRE DOS CLIENTES ---
  private clientesSubject = new BehaviorSubject<Cliente[]>([]);
  public clientes$ = this.clientesSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 1. Encher o Cofre (Usado pelo ecrã de Gestão de Clientes)
  // 🚀 ADICIONADO: Paginação para garantir que vêm todos (ex: tamanho=1000)
  carregarClientesDaAPI(pagina: number = 0, tamanho: number = 1000): void {
    this.http.get<Page<Cliente>>(`${this.API_URL}?page=${pagina}&size=${tamanho}`).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.clientesSubject.next(lista);
        this.logService.debug('Clientes carregados para a memória com sucesso.', lista.length)
      },
      error: (err) => this.logService.error('Erro ao carregar clientes:', err)
    });
  }

  // 2. Listar Clássico (Mantido para as dropdowns do ecrã de Vendas)
  // 🚀 TIPAGEM CORRIGIDA: Devolve uma Page de Clientes
  listar(pagina: number = 0, tamanho: number = 100): Observable<Page<Cliente>> {
    return this.http.get<Page<Cliente>>(`${this.API_URL}?page=${pagina}&size=${tamanho}`);
  }

  // 🚀 2.1 ADICIONADO: O espelho do GET /{id} do Backend
  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.API_URL}/${id}`);
  }

  // 3. Criar (Atualiza a memória na hora)
  // 🚀 TIPAGEM: Partial<Cliente> porque o novo cliente ainda não tem ID
  criar(cliente: Partial<Cliente>): Observable<Cliente> {
    return this.http.post<Cliente>(this.API_URL, cliente).pipe(
      tap((novoCliente) => {
        const lista = this.clientesSubject.getValue();
        this.clientesSubject.next([novoCliente, ...lista]); // Adiciona ao topo da lista
      })
    );
  }

  // 4. Atualizar (Atualiza a memória na hora)
  atualizar(id: number, cliente: Partial<Cliente>): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.API_URL}/${id}`, cliente).pipe(
      tap((clienteAtualizado) => {
        const lista = this.clientesSubject.getValue();
        this.clientesSubject.next(lista.map(c => c.id === id ? clienteAtualizado : c));
      })
    );
  }

  // 5. Apagar (Atualiza a memória na hora)
  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const lista = this.clientesSubject.getValue();
        this.clientesSubject.next(lista.filter(c => c.id !== id)); // Remove da lista visualmente
      })
    );
  }
}