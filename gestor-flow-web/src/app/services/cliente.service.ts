import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cliente } from '../core/models/cliente.model';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private readonly API_URL = 'http://localhost:8080/api/clientes';

  constructor(private http: HttpClient) { }

  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  criar(cliente: Cliente): Observable<Cliente> {
    return this.http.post<Cliente>(this.API_URL, cliente);
  }

  atualizar(id: number, cliente: Cliente): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.API_URL}/${id}`, cliente);
  }

  apagar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}