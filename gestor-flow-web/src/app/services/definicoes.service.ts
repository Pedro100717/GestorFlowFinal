import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs'; // 🚀 ADICIONADO O TAP
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

  // 🚀 TIPAGEM CORRIGIDA E UX REATIVA: Atualiza a memória local após o sucesso!
  atualizarPerfil(dados: PerfilUtilizadorDTO): Observable<PerfilUtilizadorDTO> {
    return this.http.put<PerfilUtilizadorDTO>(`${this.API_URL}/perfil`, dados).pipe(
      tap((perfilAtualizado) => {
        // Assume que o backend devolve o perfil atualizado.
        // Ao atualizarmos o localStorage, a tua Sidebar ou Topbar (que lêem de lá)
        // vão mostrar o novo nome imediatamente, sem precisar de F5!
        if (perfilAtualizado && perfilAtualizado.nome) {
          localStorage.setItem('userName', perfilAtualizado.nome);
        }
      })
    );
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