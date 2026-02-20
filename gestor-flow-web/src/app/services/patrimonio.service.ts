import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Patrimonio } from '../core/models/patrimonio.model';

@Injectable({
  providedIn: 'root'
})
export class PatrimonioService {

  private readonly API_URL = 'http://localhost:8080/api/patrimonio';

  constructor(private http: HttpClient) { }

  listar(): Observable<Patrimonio[]> {
    return this.http.get<Patrimonio[]>(this.API_URL);
  }

  criarViatura(dados: any): Observable<any> {
    return this.http.post(`${this.API_URL}/viaturas`, dados);
  }

  criarImovel(dados: any): Observable<any> {
    return this.http.post(`${this.API_URL}/imoveis`, dados);
  }

  criarFerramenta(dados: any): Observable<any> {
    return this.http.post(`${this.API_URL}/ferramentas`, dados);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}