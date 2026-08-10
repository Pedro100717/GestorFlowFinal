import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../core/models/conta-corrente.model';

@Injectable({
  providedIn: 'root'
})
export class ContaCorrenteService {

  private readonly API_URL = `${environment.apiUrl}/contas-correntes`;
  private readonly API_REPORTS_URL = `${environment.apiUrl}/reports`; 

  constructor(private http: HttpClient) { }

  // ==========================================
  // CLIENTES
  // ==========================================

  obterResumoClientes(): Observable<ContaCorrenteResumo[]> {
    return this.http.get<ContaCorrenteResumo[]>(`${this.API_URL}/clientes/resumo`);
  }

  // 🚀 ADICIONADO: Filtros opcionais de data com HttpParams
  obterExtratoCliente(clienteId: number, dataInicio?: string, dataFim?: string): Observable<ContaCorrenteExtrato[]> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);

    return this.http.get<ContaCorrenteExtrato[]>(`${this.API_URL}/clientes/${clienteId}/extrato`, { params });
  }

  // 🚀 ADICIONADO: Os PDFs também precisam de respeitar as datas escolhidas no ecrã!
  extrairPdfExtratoCliente(clienteId: number, dataInicio?: string, dataFim?: string): Observable<Blob> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);

    return this.http.get(`${this.API_REPORTS_URL}/conta-corrente/cliente/pdf/${clienteId}`, { 
      responseType: 'blob',
      params 
    });
  }

  // ==========================================
  // FORNECEDORES
  // ==========================================

  obterResumoFornecedores(): Observable<ContaCorrenteResumo[]> {
    return this.http.get<ContaCorrenteResumo[]>(`${this.API_URL}/fornecedores/resumo`);
  }

  // 🚀 ADICIONADO: Filtros opcionais de data
  obterExtratoFornecedor(fornecedorId: number, dataInicio?: string, dataFim?: string): Observable<ContaCorrenteExtrato[]> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);

    return this.http.get<ContaCorrenteExtrato[]>(`${this.API_URL}/fornecedores/${fornecedorId}/extrato`, { params });
  }

  // 🚀 ADICIONADO: Os PDFs também filtrados
  extrairPdfExtratoFornecedor(fornecedorId: number, dataInicio?: string, dataFim?: string): Observable<Blob> {
    let params = new HttpParams();
    if (dataInicio) params = params.set('dataInicio', dataInicio);
    if (dataFim) params = params.set('dataFim', dataFim);

    return this.http.get(`${this.API_REPORTS_URL}/conta-corrente/fornecedor/pdf/${fornecedorId}`, { 
      responseType: 'blob',
      params 
    });
  }
}