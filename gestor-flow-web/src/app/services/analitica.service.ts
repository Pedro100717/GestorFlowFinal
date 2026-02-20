import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CentroCusto, SeccaoHomo } from '../core/models/analitica.model';

@Injectable({
  providedIn: 'root'
})
export class AnaliticaService {

  // Aponta para os teus novos controllers
  private readonly API_CC = 'http://localhost:8080/api/centros-custo';
  private readonly API_SH = 'http://localhost:8080/api/seccoes-homogeneas';

  constructor(private http: HttpClient) { }

  // ==========================================
  // CENTROS DE CUSTO
  // ==========================================
  listarCentros(): Observable<CentroCusto[]> {
    return this.http.get<CentroCusto[]>(this.API_CC);
  }

  criarCentro(dto: CentroCusto): Observable<CentroCusto> {
    return this.http.post<CentroCusto>(this.API_CC, dto);
  }

  atualizarCentro(id: number, dto: CentroCusto): Observable<CentroCusto> {
    return this.http.put<CentroCusto>(`${this.API_CC}/${id}`, dto);
  }

  eliminarCentro(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_CC}/${id}`);
  }

  // ==========================================
  // SECÇÕES HOMOGÉNEAS
  // ==========================================
  listarSeccoes(): Observable<SeccaoHomo[]> {
    return this.http.get<SeccaoHomo[]>(this.API_SH);
  }

  criarSeccao(dto: SeccaoHomo): Observable<SeccaoHomo> {
    return this.http.post<SeccaoHomo>(this.API_SH, dto);
  }

  atualizarSeccao(id: number, dto: SeccaoHomo): Observable<SeccaoHomo> {
    return this.http.put<SeccaoHomo>(`${this.API_SH}/${id}`, dto);
  }

  eliminarSeccao(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_SH}/${id}`);
  }
}