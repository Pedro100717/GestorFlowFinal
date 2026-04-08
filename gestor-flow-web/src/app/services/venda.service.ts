import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, shareReplay, tap } from 'rxjs';
import { Venda } from '../core/models/venda.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class VendaService {

  private readonly API_URL = `${environment.apiUrl}/vendas`;

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  // 1. O "Cofre" fechado onde guardamos a lista atual de Vendas (começa vazio)
  private vendasSubject = new BehaviorSubject<Venda[]>([]);
  
  // 2. A "Montra" pública (Observable) para os ecrãs ficarem a ver as alterações em tempo real
  public vendas$ = this.vendasSubject.asObservable();

  // Cache para o IVA (Dados de Referência que já falámos)
  private cacheTaxasIva$: Observable<any[]> | null = null;

  constructor(private http: HttpClient) { }

  // 3. O método que vai ao Java e ENCHE o cofre
  carregarVendasDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.vendasSubject.next(lista); // Atualiza o cofre com os dados do Java!
      },
      error: (err) => console.error('Erro ao carregar vendas:', err)
    });
  }

  // 4. Registar Venda (Vai ao Java e, se der sucesso, atualiza o cofre na memória!)
  registar(venda: Venda): Observable<Venda> {
    return this.http.post<Venda>(this.API_URL, venda).pipe(
      tap((novaVendaRegistada) => {
        // Pega na lista atual do cofre
        const listaAtual = this.vendasSubject.getValue();
        // Coloca a nova venda no TOPO da lista e guarda de volta no cofre
        this.vendasSubject.next([novaVendaRegistada, ...listaAtual]);
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