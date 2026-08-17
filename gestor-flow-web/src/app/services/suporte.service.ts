import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BugReportDTO, ReportSuporte } from '../core/models/suporte.model'; // Ajusta o caminho

@Injectable({
  providedIn: 'root'
})
export class SuporteService {

  constructor(private http: HttpClient) { }

  submeterTicket(ticket: BugReportDTO): Observable<string> {
    return this.http.post('/api/suporte', ticket, { responseType: 'text' });
  }

  // 🚀 O NOVO MÉTODO DO BACKOFFICE
  listarTickets(): Observable<ReportSuporte[]> {
    return this.http.get<ReportSuporte[]>('/api/suporte/tickets');
  }

  apagarTicket(id: number): Observable<void> {
    return this.http.delete<void>(`/api/suporte/${id}`);
  }
}