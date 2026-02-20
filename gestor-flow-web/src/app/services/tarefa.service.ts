import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tarefa, EstadoTarefa } from '../core/models/tarefa.model';

@Injectable({
  providedIn: 'root'
})
export class TarefaService {

  private readonly API_URL = 'http://localhost:8080/api/tarefas';

  constructor(private http: HttpClient) { }

  listar(): Observable<any> {
    // Traz todas as tarefas (paginadas ou lista simples dependendo do backend)
    // Vamos assumir que o backend devolve Page, mas aqui facilitamos
    return this.http.get<any>(this.API_URL);
  }

  criar(tarefa: Tarefa): Observable<Tarefa> {
    return this.http.post<Tarefa>(this.API_URL, tarefa);
  }

  atualizar(id: number, tarefa: Tarefa): Observable<Tarefa> {
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefa);
  }

  mudarEstado(id: number, tarefa: Tarefa, novoEstado: EstadoTarefa): Observable<Tarefa> {
    // Pequeno truque: enviamos o objeto todo atualizado com o novo estado
    const tarefaAtualizada = { ...tarefa, estado: novoEstado };
    return this.http.put<Tarefa>(`${this.API_URL}/${id}`, tarefaAtualizada);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}