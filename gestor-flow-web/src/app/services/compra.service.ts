import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs';
import { Compra, TaxaIva } from '../core/models/compra.model'; // 🛡️ Tipagem forte ativada
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CompraService {

  private readonly API_URL = `${environment.apiUrl}/compras`;

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  private comprasSubject = new BehaviorSubject<Compra[]>([]);
  public compras$ = this.comprasSubject.asObservable();

  // 🛡️ ADEUS ANY! Cache agora protegida
  private cacheTaxasIva$: Observable<TaxaIva[]> | null = null;

  constructor(private http: HttpClient) { }

  carregarComprasDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.comprasSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar compras:', err)
    });
  }

  // 🛡️ Parâmetro de entrada tipado
  registar(compra: Compra): Observable<Compra> {
    return this.http.post<Compra>(this.API_URL, compra).pipe(
      tap((novaCompraRegistada) => {
        const listaAtual = this.comprasSubject.getValue();
        this.comprasSubject.next([novaCompraRegistada, ...listaAtual]);
      })
    );
  }

  // 🛡️ Parâmetro de entrada tipado
  atualizar(id: number, compra: Compra): Observable<Compra> {
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

  // 🛡️ Contrato garantido: Retorna TaxaIva[]
  listarTaxasIva(): Observable<TaxaIva[]> {
    if (!this.cacheTaxasIva$) {
      this.cacheTaxasIva$ = this.http.get<TaxaIva[]>(`${this.API_URL}/taxas-iva`).pipe(
        shareReplay(1)
      );
    }
    return this.cacheTaxasIva$;
  }
}