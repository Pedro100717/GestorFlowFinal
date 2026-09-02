import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { ContaBancaria, Movimento, DocumentoPendente } from '../core/models/tesouraria.model';
import { environment } from '../../environments/environment';
import { LogService } from '../core/services/log.service';

// 🚀 CONTRATO DO SIMULADOR
export interface PontoSimulacao {
  label: string;
  saldoProjetado: number;
}

export interface SimuladorTesourariaDTO {
  saldoAtual: number;
  pontos: PontoSimulacao[];
}

// 🚀 A INTERFACE DE PAGINAÇÃO
export interface PaginaSpring<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// 🚀 DTOs DE COMANDO (Garantem que o Frontend envia exatamente o que o Java espera)
export interface TransferenciaDTO {
  contaOrigemId: number;
  contaDestinoId: number;
  valor: number;
  descricao?: string;
}

export interface ConfirmacaoPagamentoDTO {
  documentoId: number;
  tipoDocumento: string;
  contaBancariaId: number;
  dataPagamento: string;
  valorAPagar: number;
}

@Injectable({
  providedIn: 'root'
})
export class TesourariaService {

  private readonly API_URL = `${environment.apiUrl}/tesouraria`;
  private readonly REPORTS_URL = `${environment.apiUrl}/reports`;

  // --- OS COFRES PRINCIPAIS ---
  private contasSubject = new BehaviorSubject<ContaBancaria[]>([]);
  public contas$ = this.contasSubject.asObservable();

  private movimentosSubject = new BehaviorSubject<Movimento[]>([]);
  public movimentos$ = this.movimentosSubject.asObservable();

  // --- A NOVA SUPER CACHE ---
  private extratosCache = new Map<number, Movimento[]>();
  
  private contaAtivaId: number | null = null;
  
  // 🚀 ADICIONADO: Flag de segurança para não apagar a UI e evitar SPAM de HTTP GET
  private contasCarregadas = false;

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // =========================================================================
  // --- 🚀 MÁQUINA DO TEMPO (SIMULADOR DE TESOURARIA) ---
  // =========================================================================

  obterSimulacao(): Observable<SimuladorTesourariaDTO> {
    return this.http.get<SimuladorTesourariaDTO>(`${this.API_URL}/simulador`);
  }

  // 🚀 CORRIGIDO: O Serviço recebe os dados estruturados e delega no HttpParams a segurança
  extrairEvolucaoPdf(fluxo: string, natureza: string, periodo: string): Observable<Blob> {
    const params = new HttpParams()
      .set('fluxo', fluxo)
      .set('natureza', natureza)
      .set('periodo', periodo);

    return this.http.get(`${this.REPORTS_URL}/tesouraria/evolucao/pdf`, {
      params,
      responseType: 'blob'
    });
  }

  // =========================================================================
  // --- FUNÇÕES CORE DA TESOURARIA ---
  // =========================================================================

  carregarContasDaAPI(forcarRecarregamento: boolean = false): void {
    // 🚀 BLOQUEIO: Se já carregou e não estamos a forçar, devolvemos a cache imediatamente!
    if (this.contasCarregadas && !forcarRecarregamento) {
      return; 
    }

    this.http.get<ContaBancaria[]>(`${this.API_URL}/contas`).subscribe({
      next: (dados) => {
        this.contasSubject.next(dados);
        this.contasCarregadas = true; // 🚀 Marca como carregado
        this.logService.debug('Contas bancárias carregadas com sucesso.', dados.length);
      },
      error: (e) => this.logService.error('Erro ao carregar contas:', e)
    });
  }

  // 🚀 CORRIGIDO: Adição de parâmetros de paginação (ex: 500 registos para a vista de extrato)
  obterExtrato(contaId: number, pagina: number = 0, tamanho: number = 500): void {
    this.contaAtivaId = contaId;

    if (this.extratosCache.has(contaId)) {
      this.movimentosSubject.next(this.extratosCache.get(contaId)!);
      return;
    }

    this.movimentosSubject.next([]);

    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<PaginaSpring<Movimento>>(`${this.API_URL}/contas/${contaId}/extrato`, { params }).subscribe({
      next: (dados) => {
        const lista = dados.content || [];
        this.extratosCache.set(contaId, lista);
        if (this.contaAtivaId === contaId) {
          this.movimentosSubject.next(lista);
        }
        this.logService.debug(`Extrato da conta ${contaId} carregado com sucesso.`, lista.length)
      },
      error: (e) => this.logService.error('Erro ao carregar extrato:', e)
    });
  }

  // 🚀 CORRIGIDO: Partial<ContaBancaria>
  criarConta(conta: Partial<ContaBancaria>): Observable<ContaBancaria> {
    return this.http.post<ContaBancaria>(`${this.API_URL}/contas`, conta).pipe(
      tap((novaConta) => {
        const lista = this.contasSubject.getValue();
        this.contasSubject.next([...lista, novaConta]); 
      })
    );
  }

  // 🚀 CORRIGIDO: Partial<Movimento>
  registarMovimento(movimento: Partial<Movimento>): Observable<Movimento> {
    return this.http.post<Movimento>(`${this.API_URL}/movimentos`, movimento).pipe(
      tap((novoMovimento) => {
        const contas = this.contasSubject.getValue();
        this.contasSubject.next(contas.map(c => {
          if (c.id === movimento.contaId) {
            const variacao = movimento.tipo === 'CREDITO' ? (movimento.valor || 0) : -(movimento.valor || 0);
            return { ...c, saldo: c.saldo + variacao };
          }
          return c;
        }));

        if (novoMovimento.contaId && this.extratosCache.has(novoMovimento.contaId)) {
           const movsAtuais = this.extratosCache.get(novoMovimento.contaId)!;
           const novaLista = [novoMovimento, ...movsAtuais];
           this.extratosCache.set(novoMovimento.contaId, novaLista);
           
           if (this.contaAtivaId === novoMovimento.contaId) {
             this.movimentosSubject.next(novaLista);
           }
        }
      })
    );
  }

  // 🚀 CORRIGIDO: Uso do DTO tipado
  realizarTransferencia(dados: TransferenciaDTO): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/transferencias`, dados).pipe(
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
  // --- COMUNICAÇÃO E SEGREGAÇÃO DE FUNÇÕES ---
  // =========================================================================

  // 🚀 CORRIGIDO: Tipagem da lista de Pendentes
  listarPendentes(): Observable<DocumentoPendente[]> {
    return this.http.get<DocumentoPendente[]>(`${this.API_URL}/pendentes`);
  }

  // 🚀 CORRIGIDO: Uso do DTO de confirmação e injeção da chave de Idempotência
  confirmarTransacao(dados: ConfirmacaoPagamentoDTO, idempotencyKey: string): Observable<void> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    
    return this.http.post<void>(`${this.API_URL}/confirmar-pagamento`, dados, { headers }).pipe(
      tap(() => this.notificarNovaTransacao())
    );
  }

  anularMovimento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/movimentos/${id}`).pipe(
      tap(() => this.notificarNovaTransacao())
    );
  }

  notificarNovaTransacao(): void {
    this.carregarContasDaAPI(true); // 🚀 Passa 'true' para forçar ir à BD buscar os novos saldos
    this.extratosCache.clear(); 
    
    if(this.contaAtivaId) {
        this.obterExtrato(this.contaAtivaId);
    }
  }

  // 🚀 CORRIGIDO: Payload estruturado na chamada PATCH
  alterarDataPrevista(id: number, tipo: string, novaData: string): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/previsao/${tipo}/${id}`, { novaData });
  }
}