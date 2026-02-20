import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Orcamento } from '../core/models/orcamento.model';

@Injectable({
  providedIn: 'root'
})
export class OrcamentoService {

  private readonly API_URL = 'http://localhost:8080/api/orcamentos';

  constructor(private http: HttpClient) { }

  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  buscarPorId(id: number): Observable<Orcamento> {
    return this.http.get<Orcamento>(`${this.API_URL}/${id}`);
  }

  criar(orcamento: Orcamento): Observable<Orcamento> {
    return this.http.post<Orcamento>(this.API_URL, orcamento);
  }

  atualizar(id: number, orcamento: Orcamento): Observable<Orcamento> {
    return this.http.put<Orcamento>(`${this.API_URL}/${id}`, orcamento);
  }

  alterarEstado(id: number, estado: string): Observable<Orcamento> {
    return this.http.patch<Orcamento>(`${this.API_URL}/${id}/estado?estado=${estado}`, {});
  }

  converterEmVenda(id: number): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/${id}/converter`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}