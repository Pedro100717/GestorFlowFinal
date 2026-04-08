import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Fornecedor } from '../core/models/fornecedor.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FornecedorService {

  private readonly API_URL = `${environment.apiUrl}/fornecedores`;

  // --- O NOVO COFRE DOS FORNECEDORES ---
  private fornecedoresSubject = new BehaviorSubject<Fornecedor[]>([]);
  public fornecedores$ = this.fornecedoresSubject.asObservable();

  constructor(private http: HttpClient) { }

  // 1. Encher o Cofre (Usado pelo ecrã de Gestão de Fornecedores)
  carregarFornecedoresDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        // Suporte para lista simples ou paginada, por segurança
        const lista = Array.isArray(dados) ? dados : (dados.content || []);
        this.fornecedoresSubject.next(lista);
      },
      error: (e) => console.error('Erro ao carregar fornecedores:', e)
    });
  }

  // 2. Listar Clássico (Mantido para não partir a dropdown das Compras!)
  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  // 3. Criar (Atualiza a memória na hora)
  criar(fornecedor: Fornecedor): Observable<Fornecedor> {
    return this.http.post<Fornecedor>(this.API_URL, fornecedor).pipe(
      tap((novoFornecedor) => {
        const lista = this.fornecedoresSubject.getValue();
        this.fornecedoresSubject.next([novoFornecedor, ...lista]);
      })
    );
  }

  // 4. Atualizar (Atualiza a memória na hora)
  atualizar(id: number, fornecedor: Fornecedor): Observable<Fornecedor> {
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