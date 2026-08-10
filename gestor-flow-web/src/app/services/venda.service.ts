import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Venda } from '../core/models/venda.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

// 🚀 O CONTRATO ESTANDARDIZADO
export interface PaginaSpring<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class VendaService {

  private readonly API_URL = `${environment.apiUrl}/vendas`;
  private readonly REPORTS_URL = `${environment.apiUrl}/reports`; // 🚀 Rota para os PDFs

  private vendasSubject = new BehaviorSubject<Venda[]>([]);
  public vendas$ = this.vendasSubject.asObservable();

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 🚀 CORRIGIDO: Paginação segura injetada na chamada
  carregarVendasDaAPI(pagina: number = 0, tamanho: number = 100): void {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<PaginaSpring<Venda>>(this.API_URL, { params }).subscribe({
      next: (dados) => {
        // Agora o TS sabe que a propriedade content existe!
        const lista = dados.content || [];
        this.vendasSubject.next(lista);
        this.logService.debug('Vendas carregadas para a memória com sucesso.', lista.length)
      },
      error: (err) => this.logService.error('Erro ao carregar vendas:', err)
    });
  }

  // 🚀 ADICIONADO: A lupa para consultar uma fatura específica
  buscarPorId(id: number): Observable<Venda> {
    return this.http.get<Venda>(`${this.API_URL}/${id}`);
  }

  // 🚀 CORRIGIDO: Partial para não forçar o envio de campos autogerados (ID, nº de documento)
  registar(venda: Partial<Venda>): Observable<Venda> { 
    return this.http.post<Venda>(this.API_URL, venda).pipe(
      tap((novaVendaRegistada) => {
        const listaAtual = this.vendasSubject.getValue();
        this.vendasSubject.next([novaVendaRegistada, ...listaAtual]);
      })
    );
  }

  // 🚀 CORRIGIDO: Partial aplicado aqui também
  atualizar(id: number, venda: Partial<Venda>): Observable<Venda> { 
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

  // 🚀 ADICIONADO: Método vital para extração de PDFs em sistemas de faturação!
  abrirPdfFatura(id: number): Observable<Blob> {
    return this.http.get(`${this.REPORTS_URL}/vendas/pdf/${id}`, { responseType: 'blob' });
  }

  // 🗑️ ATENÇÃO: O listarTaxasIva() foi apagado. 
  // No componente de Vendas (venda.ts), injeta o IvaService no construtor 
  // e consome o this.ivaService.listar() para preencheres os teus formulários!
}