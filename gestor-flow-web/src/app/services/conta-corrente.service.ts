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
  private readonly API_REPORTS_URL = `${environment.apiUrl}/reports`; // 🚀 Apontador para PDFs

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

  extrairPdfExtratoCliente(clienteId: number): Observable<Blob> {
    return this.http.get(`${this.API_REPORTS_URL}/conta-corrente/cliente/pdf/${clienteId}`, { 
      responseType: 'blob' 
    });
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

  // 🚀 NOVO: Extração do Extrato de Fornecedores em PDF
  extrairPdfExtratoFornecedor(fornecedorId: number): Observable<Blob> {
    return this.http.get(`${this.API_REPORTS_URL}/conta-corrente/fornecedor/pdf/${fornecedorId}`, { 
      responseType: 'blob' 
    });
  }
}