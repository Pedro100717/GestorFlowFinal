import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs';
import { Venda, TaxaIva } from '../core/models/venda.model'; // 🛡️ Importação limpa
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class VendaService {

  private readonly API_URL = `${environment.apiUrl}/vendas`;

  private vendasSubject = new BehaviorSubject<Venda[]>([]);
  public vendas$ = this.vendasSubject.asObservable();

  // 🛡️ Cache agora fortemente tipada
  private cacheTaxasIva$: Observable<TaxaIva[]> | null = null;

  constructor(private http: HttpClient) { }

  carregarVendasDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.vendasSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar vendas:', err)
    });
  }

  registar(venda: Venda): Observable<Venda> { // 🛡️ Tipado
    return this.http.post<Venda>(this.API_URL, venda).pipe(
      tap((novaVendaRegistada) => {
        const listaAtual = this.vendasSubject.getValue();
        this.vendasSubject.next([novaVendaRegistada, ...listaAtual]);
      })
    );
  }

  atualizar(id: number, venda: Venda): Observable<Venda> { // 🛡️ Tipado
    return this.http.put<Venda>(`${this.API_URL}/${id}`, venda).pipe(
      tap((vendaAtualizada) => {
        const listaAtual = this.vendasSubject.getValue();
        const index = listaAtual.findIndex(v => v.id === id);
        if (index !== -1) {
          listaAtual[index] = vendaAtualizada;
          this.vendasSubject.next([...listaAtual]);
        }
      })
    );
  }

  anular(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const listaAtual = this.vendasSubject.getValue();
        this.vendasSubject.next(listaAtual.filter(v => v.id !== id));
      })
    );
  }

  // 🛡️ Contrato garantido: Retorna sempre um array de TaxaIva
  listarTaxasIva(): Observable<TaxaIva[]> {
    if (!this.cacheTaxasIva$) {
      this.cacheTaxasIva$ = this.http.get<TaxaIva[]>(`${this.API_URL}/taxas-iva`).pipe(
        shareReplay(1)
      );
    }
    return this.cacheTaxasIva$;
  }
}