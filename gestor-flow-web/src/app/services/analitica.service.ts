import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import { CentroCusto, SeccaoHomo } from '../core/models/analitica.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AnaliticaService {

  // Aponta para os teus novos controllers
  private readonly API_CC = `${environment.apiUrl}/centros-custo`;
  private readonly API_SH = `${environment.apiUrl}/seccoes-homogeneas`;

  private cacheCentros$: Observable<CentroCusto[]> | null = null;
  private cacheSeccoes$: Observable<SeccaoHomo[]> | null = null;

  constructor(private http: HttpClient) { }

  // ==========================================
  // CENTROS DE CUSTO
  // ==========================================
  listarCentros(): Observable<CentroCusto[]> {
    if(!this.cacheCentros$) {
      this.cacheCentros$ = this.http.get<CentroCusto[]>(this.API_CC).pipe(
        shareReplay(1)
      );
    }
    return this.cacheCentros$;
  }

  criarCentro(dto: CentroCusto): Observable<CentroCusto> {
    return this.http.post<CentroCusto>(this.API_CC, dto).pipe(
      tap(() => this.cacheCentros$ = null) // Limpa o cache para forçar atualização na próxima listagem
    );
  }

  atualizarCentro(id: number, dto: CentroCusto): Observable<CentroCusto> {
    return this.http.put<CentroCusto>(`${this.API_CC}/${id}`, dto).pipe(
      tap(() => this.cacheCentros$ = null) // Limpa o cache para forçar atualização na próxima listagem
    );
  }

  eliminarCentro(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_CC}/${id}`).pipe(
      tap(() => this.cacheCentros$ = null) // Limpa o cache para forçar atualização na próxima listagem
    );
  }

  // ==========================================
  // SECÇÕES HOMOGÉNEAS
  // ==========================================
  listarSeccoes(): Observable<SeccaoHomo[]> {
    if(!this.cacheSeccoes$) {
      this.cacheSeccoes$ = this.http.get<SeccaoHomo[]>(this.API_SH).pipe(
        shareReplay(1)
      );
    }
    return this.cacheSeccoes$;
  }

  criarSeccao(dto: SeccaoHomo): Observable<SeccaoHomo> {
    return this.http.post<SeccaoHomo>(this.API_SH, dto).pipe(
      tap(() => this.cacheSeccoes$ = null)
    ); 
  }

  atualizarSeccao(id: number, dto: SeccaoHomo): Observable<SeccaoHomo> {
    return this.http.put<SeccaoHomo>(`${this.API_SH}/${id}`, dto).pipe(
      tap(() => this.cacheSeccoes$ = null)
    );
  }

  eliminarSeccao(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_SH}/${id}`).pipe(
      tap(() => this.cacheSeccoes$ = null)
    );
  }
}