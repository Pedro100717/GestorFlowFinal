// conta-corrente.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../core/models/conta-corrente.model';

@Injectable({
  providedIn: 'root'
})
export class ContaCorrenteService {

  private readonly API_URL = `${environment.apiUrl}/contas-correntes`;

  constructor(private http: HttpClient) { }

  // ==========================================
  // CLIENTES
  // ==========================================

  obterResumoClientes(): Observable<ContaCorrenteResumo[]> {
    return this.http.get<ContaCorrenteResumo[]>(`${this.API_URL}/clientes/resumo`);
  }

  obterExtratoCliente(clienteId: number): Observable<ContaCorrenteExtrato[]> {
    return this.http.get<ContaCorrenteExtrato[]>(`${this.API_URL}/clientes/${clienteId}/extrato`);
  }

  // ==========================================
  // FORNECEDORES
  // ==========================================

  obterResumoFornecedores(): Observable<ContaCorrenteResumo[]> {
    return this.http.get<ContaCorrenteResumo[]>(`${this.API_URL}/fornecedores/resumo`);
  }

  obterExtratoFornecedor(fornecedorId: number): Observable<ContaCorrenteExtrato[]> {
    return this.http.get<ContaCorrenteExtrato[]>(`${this.API_URL}/fornecedores/${fornecedorId}/extrato`);
  }
}