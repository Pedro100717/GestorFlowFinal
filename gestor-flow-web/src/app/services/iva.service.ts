import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TxIva } from '../core/models/iva.model'; // 🚀 O TEU NOVO IMPORT

@Injectable({
  providedIn: 'root'
})
export class IvaService {
  
  private apiUrl = `${environment.apiUrl}/iva`; 

  constructor(private http: HttpClient) { }

  listar(): Observable<TxIva[]> {
    return this.http.get<TxIva[]>(this.apiUrl);
  }
}