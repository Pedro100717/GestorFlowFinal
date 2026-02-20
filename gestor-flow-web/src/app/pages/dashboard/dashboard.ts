import { Component, OnInit, ChangeDetectorRef } from '@angular/core'; // <--- Importar ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {

  dataHoje: Date = new Date();
  
  totalVendas: number = 0;
  totalClientes: number = 0;
  valorStock: number = 0;
  totalCompras: number = 0;
  
  ultimasVendas: any[] = [];

  constructor(
    private http: HttpClient,
    private cd: ChangeDetectorRef // <--- Injetar o detetor de mudanças
  ) {}

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    this.http.get<any>('http://localhost:8080/api/dashboard/resumo').subscribe({
      next: (dados) => {
        console.log('Dados recebidos:', dados); // Para confirmares na consola

        // Atualizar variáveis
        this.totalVendas = dados.totalVendas || 0;
        this.valorStock = dados.valorStock || 0;
        this.totalClientes = dados.totalClientes || 0;
        this.totalCompras = dados.totalCompras || 0;
        this.ultimasVendas = dados.ultimasVendas || [];

        // O SEGREDO: Forçar o Angular a atualizar o HTML agora mesmo!
        this.cd.detectChanges(); 
      },
      error: (e) => console.error('Erro dashboard:', e)
    });
  }
}