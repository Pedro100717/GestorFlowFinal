import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Compra } from '../core/models/compra.model';

@Injectable({
  providedIn: 'root'
})
export class CompraService {

  private readonly API_URL = 'http://localhost:8080/api/compras';

  constructor(private http: HttpClient) { }

  listar(): Observable<any> {
    return this.http.get<any>(this.API_URL);
  }

  registar(compra: Compra): Observable<Compra> {
    return this.http.post<Compra>(this.API_URL, compra);
  }

  listarTaxasIva(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/taxas-iva`);
  }
}