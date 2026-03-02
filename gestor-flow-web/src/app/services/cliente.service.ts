import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Cliente } from '../core/models/cliente.model';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private readonly API_URL = 'http://localhost:8080/api/clientes';

  // --- O NOVO COFRE DOS CLIENTES ---
  private clientesSubject = new BehaviorSubject<Cliente[]>([]);
  public clientes$ = this.clientesSubject.asObservable();

  constructor(private http: HttpClient) { }

  // 1. Encher o Cofre (Usado pelo ecrã de Gestão de Clientes)
  carregarClientesDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.clientesSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar clientes:', err)
    });
  }

  // 2. Listar Clássico (Mantido para as dropdowns do ecrã de Vendas)
  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  // 3. Criar (Atualiza a memória na hora)
  criar(cliente: Cliente): Observable<Cliente> {
    return this.http.post<Cliente>(this.API_URL, cliente).pipe(
      tap((novoCliente) => {
        const lista = this.clientesSubject.getValue();
        this.clientesSubject.next([novoCliente, ...lista]); // Adiciona ao topo da lista
      })
    );
  }

  // 4. Atualizar (Atualiza a memória na hora)
  atualizar(id: number, cliente: Cliente): Observable<Cliente> {
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