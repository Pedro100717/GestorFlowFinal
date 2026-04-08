import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs'; // <--- NOVOS IMPORTS
import { Compra } from '../core/models/compra.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CompraService {

  private readonly API_URL = `${environment.apiUrl}/compras`;

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  // 1. O "Cofre" das compras
  private comprasSubject = new BehaviorSubject<Compra[]>([]);
  // 2. A "Montra" para o ecrã ver
  public compras$ = this.comprasSubject.asObservable();

  // Cache para o IVA
  private cacheTaxasIva$: Observable<any[]> | null = null;

  constructor(private http: HttpClient) { }

  // 3. Encher o cofre com dados da API
  carregarComprasDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.comprasSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar compras:', err)
    });
  }

  // 4. Registar e atualizar o cofre instantaneamente
  registar(compra: Compra): Observable<Compra> {
    return this.http.post<Compra>(this.API_URL, compra).pipe(
      tap((novaCompraRegistada) => {
        const listaAtual = this.comprasSubject.getValue();
        this.comprasSubject.next([novaCompraRegistada, ...listaAtual]);
      })
    );
  }

  // ==========================================
  // DADOS DE REFERÊNCIA (COM CACHE)
  // ==========================================
  listarTaxasIva(): Observable<any[]> {
    if (!this.cacheTaxasIva$) {
      this.cacheTaxasIva$ = this.http.get<any[]>(`${this.API_URL}/taxas-iva`).pipe(
        shareReplay(1)
      );
    }
    return this.cacheTaxasIva$;
  }
}