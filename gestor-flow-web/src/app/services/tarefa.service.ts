import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, catchError } from 'rxjs'; // <--- NOVO IMPORT
import { Tarefa, EstadoTarefa } from '../core/models/tarefa.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TarefaService {

  private readonly API_URL = `${environment.apiUrl}/tarefas`;

  // --- GESTÃO DE ESTADO (STATE MANAGEMENT) ---
  private tarefasSubject = new BehaviorSubject<Tarefa[]>([]); // O "cofre"
  public tarefas$ = this.tarefasSubject.asObservable(); // A "montra"

  constructor(private http: HttpClient) { }

  // 1. Encher o cofre com dados da API (chamado apenas ao abrir a página)
  carregarTarefasDaAPI(): void {
    this.http.get<any>(this.API_URL).subscribe({
      next: (dados) => {
        const lista = dados.content || dados;
        this.tarefasSubject.next(lista);
      },
      error: (err) => console.error('Erro ao carregar tarefas:', err)
    });
  }

  // 2. Criar: Adiciona à memória automaticamente
  criar(tarefa: Tarefa): Observable<Tarefa> {
    return this.http.post<Tarefa>(this.API_URL, tarefa).pipe(
      tap((novaTarefa) => {
        const listaAtual = this.tarefasSubject.getValue();
        this.tarefasSubject.next([...listaAtual, novaTarefa]); // Junta a nova ao fim da lista
      })
    );
  }

  // 3. Atualizar: Substitui a tarefa antiga pela nova na memória
  atualizar(id: number, tarefa: Tarefa): Observable<Tarefa> {
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefa).pipe(
      tap((tarefaAtualizada) => {
        const listaAtual = this.tarefasSubject.getValue();
        this.tarefasSubject.next(listaAtual.map(t => t.id === id ? tarefaAtualizada : t));
      })
    );
  }

  // 4. Mudar Estado (O "arrastar" do Kanban): Atualiza a memória na hora!
  mudarEstado(id: number, tarefa: Tarefa, novoEstado: EstadoTarefa): Observable<Tarefa> {
    
    // 1. Guardamos a lista antiga caso a internet vá abaixo e dê erro
    const listaAntiga = this.tarefasSubject.getValue();
    
    // 2. Criamos a tarefa atualizada
    const tarefaOtimista = { ...tarefa, estado: novoEstado };
    
    // 3. ATUALIZAMOS O ECRÃ IMEDIATAMENTE (Delay = 0ms)
    this.tarefasSubject.next(listaAntiga.map(t => t.id === id ? tarefaOtimista : t));

    // 4. Só agora enviamos para o Java, em background
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefaOtimista).pipe(
      catchError(erro => {
        // Se o Java der erro, revertemos o ecrã para a posição original silenciosamente
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