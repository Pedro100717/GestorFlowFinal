import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORTADO: HttpParams
import { BehaviorSubject, Observable, tap, catchError } from 'rxjs';
import { Tarefa, EstadoTarefa } from '../core/models/tarefa.model';
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
export class TarefaService {

  private readonly API_URL = `${environment.apiUrl}/tarefas`;

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  private tarefasSubject = new BehaviorSubject<Tarefa[]>([]); // O "cofre"
  public tarefas$ = this.tarefasSubject.asObservable(); // A "montra"

  constructor(
    private http: HttpClient,
    private logService: LogService
  ) { }

  // 🚀 TIPAGEM E PAGINAÇÃO: O Kanban precisa de pedir muitas tarefas (ex: size=200)
  carregarTarefasDaAPI(pagina: number = 0, tamanho: number = 200): void {
    const params = new HttpParams()
      .set('page', pagina.toString())
      .set('size', tamanho.toString());

    this.http.get<PaginaSpring<Tarefa>>(this.API_URL, { params }).subscribe({
      next: (dados) => {
        // Agora o TS sabe que é uma PaginaSpring, content existe garantidamente
        this.tarefasSubject.next(dados.content);
        this.logService.debug('Tarefas carregadas para a memória com sucesso.', dados.content.length)
      },
      error: (err) => this.logService.error('Erro ao carregar tarefas:', err)
    });
  }

  // 🚀 ADICIONADO: Para links diretos /tarefas/45
  buscarPorId(id: number): Observable<Tarefa> {
    return this.http.get<Tarefa>(`${this.API_URL}/${id}`);
  }

  // 🚀 CORRIGIDO: Partial<Tarefa> porque ainda não há ID gerado
  criar(tarefa: Partial<Tarefa>): Observable<Tarefa> {
    return this.http.post<Tarefa>(this.API_URL, tarefa).pipe(
      tap((novaTarefa) => {
        const listaAtual = this.tarefasSubject.getValue();
        // Dica UX: Colocamos a nova tarefa no início da lista, e não no fim!
        this.tarefasSubject.next([novaTarefa, ...listaAtual]); 
      })
    );
  }

  // 🚀 CORRIGIDO: Partial<Tarefa>
  atualizar(id: number, tarefa: Partial<Tarefa>): Observable<Tarefa> {
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefa).pipe(
      tap((tarefaAtualizada) => {
        const listaAtual = this.tarefasSubject.getValue();
        this.tarefasSubject.next(listaAtual.map(t => t.id === id ? tarefaAtualizada : t));
      })
    );
  }

  // 4. Mudar Estado (O "arrastar" do Kanban) - 🚀 TIPAGENS REFORÇADAS
  mudarEstado(id: number, tarefa: Partial<Tarefa>, novoEstado: EstadoTarefa): Observable<Tarefa> {
    
    const listaAntiga = this.tarefasSubject.getValue();
    const tarefaOtimista = { ...tarefa, estado: novoEstado } as Tarefa; // Cast seguro para o ecrã
    
    // ATUALIZAÇÃO OTIMISTA (Zero Lag)
    this.tarefasSubject.next(listaAntiga.map(t => t.id === id ? tarefaOtimista : t));

    // O Pedido real
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefaOtimista).pipe(
      catchError((erro) => {
        console.error('Erro ao mover a tarefa, revertendo...', erro);
        this.tarefasSubject.next(listaAntiga);
        throw erro;
      })
    );
  }

  // 5. Eliminar: Remove da memória
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        const listaAtual = this.tarefasSubject.getValue();
        this.tarefasSubject.next(listaAtual.filter(t => t.id !== id));
      })
    );
  }
}