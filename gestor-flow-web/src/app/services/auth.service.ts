import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly API_URL = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) { }

  login(credenciais: any): Observable<any> {
    return this.http.post(`${this.API_URL}/login`, credenciais);
  }

  registar(dadosRegisto: any): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, dadosRegisto);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('userName');
    localStorage.removeItem('userEmail');
  }
}