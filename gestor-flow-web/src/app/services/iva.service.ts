import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs'; // 🚀 ADICIONADO: shareReplay
import { environment } from '../../environments/environment';
import { TxIva } from '../core/models/iva.model'; 

@Injectable({
  providedIn: 'root'
})
export class IvaService {
  
  // 🚀 UNIFORME RESTAURADO: readonly e padrão de nomenclatura
  private readonly API_URL = `${environment.apiUrl}/iva`; 

  // 🚀 O NOVO COFRE DO IVA
  private cacheIva$: Observable<TxIva[]> | null = null;

  constructor(private http: HttpClient) { }

  listar(): Observable<TxIva[]> {
    // Se a cache estiver vazia, vai ao Spring Boot. Se não, devolve o que já sabe!
    if (!this.cacheIva$) {
      this.cacheIva$ = this.http.get<TxIva[]>(this.API_URL).pipe(
        shareReplay(1)
      );
    }
    return this.cacheIva$;
  }
}