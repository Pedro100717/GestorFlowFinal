import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // 🚀 ADICIONADO: Para podermos reencaminhar o utilizador
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// 🚀 ADICIONADO: Tipagem forte para blindar as entradas e saídas!
export interface LoginRequest {
  email: string;
  senha?: string; // ou 'password', dependendo do que o teu Backend espera
}

export interface RegistoRequest {
  nome: string;
  email: string;
  senha?: string;
}

export interface AuthResponse {
  token: string;
  nome: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_URL = `${environment.apiUrl}/auth`;

  // 🚀 ADICIONADO: Injeção do Router
  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  // 🚀 TIPAGEM CORRIGIDA: Adeus 'any'!
  login(credenciais: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credenciais);
  }

  // 🚀 TIPAGEM CORRIGIDA
  registar(dadosRegisto: RegistoRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, dadosRegisto);
  }

  // 🚀 UX CORRIGIDA: Limpa a casa e tranca a porta!
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userName');
    localStorage.removeItem('userEmail');
    
    // Força o utilizador a voltar para a rua (Login) imediatamente
    this.router.navigate(['/login']);
  }
}