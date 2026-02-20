import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Venda } from '../core/models/venda.model';

@Injectable({
  providedIn: 'root'
})
export class VendaService {

  private readonly API_URL = 'http://localhost:8080/api/vendas';

  constructor(private http: HttpClient) { }

  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  registar(venda: Venda): Observable<Venda> {
    return this.http.post<Venda>(this.API_URL, venda);
  }

  listarTaxasIva(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/taxas-iva`);
  }
}