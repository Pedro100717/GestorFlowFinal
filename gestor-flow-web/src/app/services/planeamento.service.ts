import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovimentoPlaneado } from '../core/models/tesouraria.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PlaneamentoService {

  private readonly API_URL = `${environment.apiUrl}/planeamento`; 

  constructor(private http: HttpClient) { }

  // =========================================================================
  // 🚀 CRUD DE PLANEAMENTO BASE
  // =========================================================================
  
  listarPlanos(): Observable<MovimentoPlaneado[]> {
    return this.http.get<MovimentoPlaneado[]>(this.API_URL);
  }

  criarPlano(plano: MovimentoPlaneado): Observable<MovimentoPlaneado> {
    return this.http.post<MovimentoPlaneado>(this.API_URL, plano);
  }

  atualizarPlano(id: number, plano: MovimentoPlaneado): Observable<MovimentoPlaneado> {
    return this.http.put<MovimentoPlaneado>(`${this.API_URL}/${id}`, plano);
  }

  apagarPlano(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  // Ativar ou desativar uma projeção no simulador sem a apagar
  alternarStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/toggle`, {});
  }

  // =========================================================================
  // 🚀 MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR)
  // =========================================================================

  // 1. Apaga/Silencia o plano apenas num mês específico
  ignorarDataPlano(id: number, dataAignorar: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/excecao?data=${dataAignorar}`);
  }

  // 2. Edita o plano apenas num mês específico (Cria uma exceção pontual)
  criarExcecaoPlano(id: number, dataOriginal: string, planoExcecao: MovimentoPlaneado): Observable<MovimentoPlaneado> {
    return this.http.post<MovimentoPlaneado>(`${this.API_URL}/${id}/excecao?data=${dataOriginal}`, planoExcecao);
  }
}