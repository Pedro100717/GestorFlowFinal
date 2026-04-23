import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ContaBancaria, Movimento } from '../core/models/tesouraria.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TesourariaService {

  private readonly API_URL = `${environment.apiUrl}/tesouraria`;

  // --- OS COFRES PRINCIPAIS ---
  private contasSubject = new BehaviorSubject<ContaBancaria[]>([]);
  public contas$ = this.contasSubject.asObservable();

  private movimentosSubject = new BehaviorSubject<Movimento[]>([]);
  public movimentos$ = this.movimentosSubject.asObservable();

  // --- A NOVA SUPER CACHE PARA TIRAR O DELAY DOS EXTRATOS ---
  private extratosCache = new Map<number, Movimento[]>();
  
  private contaAtivaId: number | null = null;

  constructor(private http: HttpClient) { }

  carregarContasDaAPI(): void {
    this.http.get<ContaBancaria[]>(`${this.API_URL}/contas`).subscribe({
      next: (dados) => this.contasSubject.next(dados),
      error: (e) => console.error('Erro ao carregar contas:', e)
    });
  }

  obterExtrato(contaId: number): void {
    this.contaAtivaId = contaId;

    if (this.extratosCache.has(contaId)) {
      this.movimentosSubject.next(this.extratosCache.get(contaId)!);
      return;
    }

    this.movimentosSubject.next([]);

    this.http.get<any>(`${this.API_URL}/contas/${contaId}/extrato`).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.extratosCache.set(contaId, lista);
        if (this.contaAtivaId === contaId) {
          this.movimentosSubject.next(lista);
        }
      },
      error: (e) => console.error('Erro ao carregar extrato:', e)
    });
  }

  criarConta(conta: any): Observable<ContaBancaria> {
    return this.http.post<ContaBancaria>(`${this.API_URL}/contas`, conta).pipe(
      tap((novaConta) => {
        const lista = this.contasSubject.getValue();
        this.contasSubject.next([...lista, novaConta]); 
      })
    );
  }

  registarMovimento(movimento: any): Observable<Movimento> {
    return this.http.post<Movimento>(`${this.API_URL}/movimentos`, movimento).pipe(
      tap((novoMovimento) => {
        const contas = this.contasSubject.getValue();
        this.contasSubject.next(contas.map(c => {
          if (c.id === movimento.contaId) {
            const variacao = movimento.tipo === 'CREDITO' ? movimento.valor : -movimento.valor;
            return { ...c, saldo: c.saldo + variacao };
          }
          return c;
        }));

        if (this.extratosCache.has(movimento.contaId)) {
           const movsAtuais = this.extratosCache.get(movimento.contaId)!;
           const novaLista = [novoMovimento, ...movsAtuais];
           this.extratosCache.set(movimento.contaId, novaLista);
           
           if (this.contaAtivaId === movimento.contaId) {
             this.movimentosSubject.next(novaLista);
           }
        }
      })
    );
  }

  realizarTransferencia(dados: any): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/transferencias`, dados).pipe(
      tap(() => {
        const contas = this.contasSubject.getValue();
        this.contasSubject.next(contas.map(c => {
          if (c.id === dados.contaOrigemId) return { ...c, saldo: c.saldo - dados.valor };
          if (c.id === dados.contaDestinoId) return { ...c, saldo: c.saldo + dados.valor };
          return c;
        }));

        this.extratosCache.delete(dados.contaOrigemId);
        this.extratosCache.delete(dados.contaDestinoId);

        if (this.contaAtivaId !== null && (this.contaAtivaId === dados.contaOrigemId || this.contaAtivaId === dados.contaDestinoId)) {
          this.obterExtrato(this.contaAtivaId);
        }
      })
    );
  }

  // =========================================================================
  // --- COMUNICAÇÃO E SEGREGAÇÃO DE FUNÇÕES (O NOVO FLUXO) ---
  // =========================================================================

  // 1. Vai buscar a lista de tudo o que as Compras e Vendas geraram mas ainda não foi pago
  listarPendentes(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/pendentes`);
  }

  // 2. Confirma a transação, gera o movimento na conta escolhida e tira o documento de "Pendente"
  confirmarTransacao(dados: any): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/confirmar-pagamento`, dados).pipe(
      tap(() => {
        // Como o dinheiro mexeu, temos de forçar a atualização dos saldos e limpar a cache
        this.notificarNovaTransacao();
      })
    );
  }

  notificarNovaTransacao(): void {
    this.carregarContasDaAPI(); 
    this.extratosCache.clear(); 
    
    // Se estivéssemos a ver um extrato, atualiza-o de imediato
    if(this.contaAtivaId) {
        this.obterExtrato(this.contaAtivaId);
    }
  }

}