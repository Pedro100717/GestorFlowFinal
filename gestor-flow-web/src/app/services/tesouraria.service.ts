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
  // Guarda um mapa na memória com os movimentos de cada conta (ex: { Conta 1: [movs], Conta 2: [movs] })
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

    // Se já temos o extrato na memória, mostra-o imediatamente!
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
        // A. Atualiza o saldo da conta matematicamente
        const contas = this.contasSubject.getValue();
        this.contasSubject.next(contas.map(c => {
          if (c.id === movimento.contaId) {
            const variacao = movimento.tipo === 'CREDITO' ? movimento.valor : -movimento.valor;
            return { ...c, saldo: c.saldo + variacao };
          }
          return c;
        }));

        // B. Injeta o movimento na Super Cache se essa conta já tiver sido carregada hoje
        if (this.extratosCache.has(movimento.contaId)) {
           const movsAtuais = this.extratosCache.get(movimento.contaId)!;
           const novaLista = [novoMovimento, ...movsAtuais];
           this.extratosCache.set(movimento.contaId, novaLista);
           
           // Se a conta estiver aberta no momento, a linha aparece instantaneamente
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
        // A. Atualiza os saldos das duas contas matematicamente
        const contas = this.contasSubject.getValue();
        this.contasSubject.next(contas.map(c => {
          if (c.id === dados.contaOrigemId) return { ...c, saldo: c.saldo - dados.valor };
          if (c.id === dados.contaDestinoId) return { ...c, saldo: c.saldo + dados.valor };
          return c;
        }));

        // B. Apaga a cache destas duas contas para forçar o Angular a ir buscar a linha nova da transferência ao Java
        this.extratosCache.delete(dados.contaOrigemId);
        this.extratosCache.delete(dados.contaDestinoId);

        if (this.contaAtivaId !== null && (this.contaAtivaId === dados.contaOrigemId || this.contaAtivaId === dados.contaDestinoId)) {
          this.obterExtrato(this.contaAtivaId);
        }
      })
    );
  }

  // =========================================================================
  // --- COMUNICAÇÃO COM OS OUTROS MÓDULOS (COMPRAS, VENDAS E ORÇAMENTOS) ---
  // =========================================================================

  // Quando fazes uma Fatura ou Pagamento noutro módulo, ele avisa a Tesouraria usando esta função:
  notificarNovaTransacao(): void {
    // 1. Vai buscar os novos saldos ao Java e atualiza o Cofre instantaneamente em todo o site
    this.carregarContasDaAPI(); 
    
    // 2. Limpa a "Super Cache" dos extratos para forçar a nova fatura a aparecer na linha do banco!
    this.extratosCache.clear(); 
  }

}