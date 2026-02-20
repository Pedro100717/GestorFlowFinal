import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovimentoStock } from '../core/models/stock.model';

@Injectable({
  providedIn: 'root'
})
export class StockService {

  private readonly API_URL = 'http://localhost:8080/api/stock';

  constructor(private http: HttpClient) { }

  listarHistorico(): Observable<any> {
    // O backend devolve uma Page, por isso usamos 'any' ou criamos uma interface Page
    return this.http.get<any>(`${this.API_URL}/historico`);
  }

  registarAcerto(acerto: MovimentoStock): Observable<MovimentoStock> {
    return this.http.post<MovimentoStock>(`${this.API_URL}/acerto`, acerto);
  }
}