import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; 
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  senha?: string; 
}

export interface RegistoRequest {
  nomeUtilizador: string; // 🚀 Alinhado com o DTO do Java
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

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  login(credenciais: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credenciais);
  }

  // 🚀 TIPAGEM ESTRITA: O Java devolve Void, o Angular espera void. Perfeito.
  registar(dadosRegisto: RegistoRequest): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/register`, dadosRegisto);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userName');
    localStorage.removeItem('userEmail');
    
    this.router.navigate(['/login']);
  }
}