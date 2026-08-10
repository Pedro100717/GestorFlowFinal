import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // 🚀 IMPORTADO: HttpParams
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
  
  // 🚀 ADICIONADO: O espelho do GET /{id} do Backend
  buscarPorId(id: number): Observable<MovimentoPlaneado> {
    return this.http.get<MovimentoPlaneado>(`${this.API_URL}/${id}`);
  }

  listarPlanos(): Observable<MovimentoPlaneado[]> {
    return this.http.get<MovimentoPlaneado[]>(this.API_URL);
  }

  // 🚀 TIPAGEM CORRIGIDA: Partial para permitir criar sem ID
  criarPlano(plano: Partial<MovimentoPlaneado>): Observable<MovimentoPlaneado> {
    return this.http.post<MovimentoPlaneado>(this.API_URL, plano);
  }

  // 🚀 TIPAGEM CORRIGIDA
  atualizarPlano(id: number, plano: Partial<MovimentoPlaneado>): Observable<MovimentoPlaneado> {
    return this.http.put<MovimentoPlaneado>(`${this.API_URL}/${id}`, plano);
  }

  apagarPlano(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  alternarStatus(id: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${id}/toggle`, {});
  }

  // =========================================================================
  // 🚀 MÁQUINA DO TEMPO: EXCEÇÕES (ESTILO GOOGLE CALENDAR)
  // =========================================================================

  // 🚀 REESCRITO: Uso de HttpParams para URLs seguros
  ignorarDataPlano(id: number, dataAignorar: string): Observable<void> {
    const params = new HttpParams().set('data', dataAignorar);
    return this.http.delete<void>(`${this.API_URL}/${id}/excecao`, { params });
  }

  // 🚀 REESCRITO: Uso de HttpParams e Partial
  criarExcecaoPlano(id: number, dataOriginal: string, planoExcecao: Partial<MovimentoPlaneado>): Observable<MovimentoPlaneado> {
    const params = new HttpParams().set('data', dataOriginal);
    return this.http.post<MovimentoPlaneado>(`${this.API_URL}/${id}/excecao`, planoExcecao, { params });
  }
}