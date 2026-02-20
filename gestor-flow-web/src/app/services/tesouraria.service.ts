import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TesourariaService {

  private readonly API_URL = 'http://localhost:8080/api/tesouraria';

  constructor(private http: HttpClient) { }

  // Lista todas as contas do utilizador
  listarContas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/contas`);
  }

  // Cria uma nova conta (envia nome, iban, saldoInicial)
  criarConta(conta: any): Observable<any> {
    return this.http.post(`${this.API_URL}/contas`, conta);
  }

  // Regista movimento (envia contaId, tipo, valor, descricao)
  registarMovimento(movimento: any): Observable<any> {
    return this.http.post(`${this.API_URL}/movimentos`, movimento);
  }

  // Obtém o histórico de movimentos de uma conta
  obterExtrato(contaId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/contas/${contaId}/extrato`);
  }
}