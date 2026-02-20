import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Artigo } from '../core/models/artigo.model';

@Injectable({
  providedIn: 'root'
})
export class ArtigoService {

  private readonly API_URL = 'http://localhost:8080/api/artigos';

  constructor(private http: HttpClient) { }

  // 1. Listar (Agora lida com Paginação)
  listar(): Observable<any> {
    // O Backend retorna Page<Artigo>, por isso usamos 'any' aqui para simplificar a extração depois
    return this.http.get<any>(this.API_URL);
  }

  // 2. Criar
  criar(artigo: Artigo): Observable<Artigo> {
    return this.http.post<Artigo>(this.API_URL, artigo);
  }

  atualizar(id: number, artigo: Artigo): Observable<Artigo> {
    return this.http.put<Artigo>(`${this.API_URL}/${id}`, artigo);
  }

  // 3. Buscar Taxas de IVA (Novo endpoint que criaste no Controller!)
  listarTaxasIva(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/taxas-iva`);
  }

  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}