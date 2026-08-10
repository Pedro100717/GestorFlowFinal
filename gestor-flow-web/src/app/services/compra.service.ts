import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO: HttpParams
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Compra } from '../core/models/compra.model'; // 🛡️ Importação do IVA foi removida
import { environment } from '../../environments/environment';

// 🚀 O CONTRATO DE PAGINAÇÃO
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
export class CompraService {

  private readonly API_URL = `${environment.apiUrl}/compras`;
  // 🗑️ API_IVA removido!

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  private comprasSubject = new BehaviorSubject<Compra[]>([]);
  public compras$ = this.comprasSubject.asObservable();

  // 🗑️ Cache do IVA removida!

  constructor(private http: HttpClient) { }

  // 🚀 CORRIGIDO: Substituição da colagem de strings suja por HttpParams seguro
  carregarComprasDaAPI(pagina: number = 0, tamanho: number = 100): void {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<Page<Compra>>(this.API_URL, { params }).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.comprasSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar compras:', err)
    });
  }

  // O espelho do GET /{id} do Backend
  buscarPorId(id: number): Observable<Compra> {
    return this.http.get<Compra>(`${this.API_URL}/${id}`);
  }

  // TIPAGEM: Partial<Compra> porque uma nova compra não tem ID
  registar(compra: Partial<Compra>): Observable<Compra> {
    return this.http.post<Compra>(this.API_URL, compra).pipe(
      tap((novaCompraRegistada) => {
        const listaAtual = this.comprasSubject.getValue();
        this.comprasSubject.next([novaCompraRegistada, ...listaAtual]);
      })
    );
  }

  // TIPAGEM: Partial<Compra>
  atualizar(id: number, compra: Partial<Compra>): Observable<Compra> {
    return this.http.put<Compra>(`${this.API_URL}/${id}`, compra).pipe(
      tap((compraAtualizada) => {
        const listaAtual = this.comprasSubject.getValue();
        const index = listaAtual.findIndex(c => c.id === id);
        if (index !== -1) {
          listaAtual[index] = compraAtualizada;
          this.comprasSubject.next([...listaAtual]); 
        }
      })
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const listaAtual = this.comprasSubject.getValue();
        this.comprasSubject.next(listaAtual.filter(c => c.id !== id));
      })
    );
  }

  // 🗑️ listarTaxasIva() removido com sucesso! O componente que use o IvaService.
}