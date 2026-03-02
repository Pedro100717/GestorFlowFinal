import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import { VendaResumo } from '../../core/models/dashboard.model';

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
  ultimasVendas: VendaResumo[] = [];

  constructor(
    private dashboardService: DashboardService,
    private cd: ChangeDetectorRef 
  ) {}

  ngOnInit() {
    // 1. Fica à escuta do Cofre. Sempre que houver dados, atualiza as variáveis.
    this.dashboardService.resumo$.subscribe(dados => {
      if (dados) {
        this.totalVendas = dados.totalVendas || 0;
        this.valorStock = dados.valorStock || 0;
        this.totalClientes = dados.totalClientes || 0;
        this.totalCompras = dados.totalCompras || 0;
        this.ultimasVendas = dados.ultimasVendas || [];
        
        this.cd.detectChanges(); 
      }
    });

    // 2. Manda o serviço ir ao Java ver se há novidades
    this.dashboardService.carregarResumo();
  }
}