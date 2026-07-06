import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// 🚀 Interfaces espelhadas exatamente do teu Spring Boot
export interface PerfilUtilizadorDTO {
  nome: string;
  email: string;
}

export interface EmpresaDTO {
  nomeFiscal: string;
  nif: string;
  moradaCompleta?: string;
  codigoPostal?: string;
  localidade?: string;
  telefone?: string;
  emailGeral?: string;
  logotipoPath?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DefinicoesService {

  private readonly API_URL = `${environment.apiUrl}/definicoes`;

  constructor(private http: HttpClient) { }

  // ==========================================
  // ENDPOINTS DO PERFIL
  // ==========================================
  obterPerfil(): Observable<PerfilUtilizadorDTO> {
    return this.http.get<PerfilUtilizadorDTO>(`${this.API_URL}/perfil`);
  }

  atualizarPerfil(dados: PerfilUtilizadorDTO): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/perfil`, dados);
  }

  // ==========================================
  // ENDPOINTS DA EMPRESA
  // ==========================================
  obterEmpresa(): Observable<EmpresaDTO> {
    return this.http.get<EmpresaDTO>(`${this.API_URL}/empresa`);
  }

  atualizarEmpresa(dados: EmpresaDTO): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/empresa`, dados);
  }

  // ==========================================
  // 🚀 UPLOAD DE LOGÓTIPO
  // ==========================================
  uploadLogo(ficheiro: File): Observable<{caminho: string}> {
    const formData = new FormData();
    formData.append('file', ficheiro);
    
    return this.http.post<{caminho: string}>(`${this.API_URL}/empresa/logo`, formData);
  }
}