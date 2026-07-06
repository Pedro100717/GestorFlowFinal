import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../../services/dashboard.service';
import { VendaResumo } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {

  dataHoje: Date = new Date();
  
  // 🚀 NOVA VARIÁVEL: Controla o estado do menu (aberto/fechado)
  dropdownAberto: boolean = false;

  // Variáveis de Controlo do Filtro
  dataInicio: string = '';
  dataFim: string = '';
  labelFiltroData: string = 'Este Mês';

  // Variáveis de Dados
  totalVendas: number = 0;
  margemBruta: number = 0;
  valorStock: number = 0;
  totalCompras: number = 0;
  ultimasVendas: VendaResumo[] = [];

  constructor(
    private dashboardService: DashboardService,
    private cd: ChangeDetectorRef 
  ) {}

  ngOnInit() {
    this.dashboardService.resumo$.subscribe(dados => {
      if (dados) {
        this.totalVendas = dados.totalVendas || 0;
        this.valorStock = dados.valorStock || 0;
        this.margemBruta = dados.margemBruta || 0;
        this.totalCompras = dados.totalCompras || 0;
        this.ultimasVendas = dados.ultimasVendas || [];
        
        this.cd.detectChanges(); 
      }
    });

    // Arranca por defeito com os dados "Deste Mês"
    this.setFiltroRapido('ESTE_MES');
  }

  // ==========================================
  // LÓGICA DE FILTRAGEM DE DATAS
  // ==========================================

  setFiltroRapido(tipo: string) {
    const hoje = new Date();
    let inicio = new Date();
    let fim = new Date();

    if (tipo === 'TUDO') {
      this.dataInicio = '';
      this.dataFim = '';
      this.labelFiltroData = 'Todo o Histórico';
      this.aplicarFiltroPersonalizado(this.labelFiltroData);
      
      this.dropdownAberto = false; // 🚀 FECHA O MENU
      return; 
    }

    switch (tipo) {
      case 'ESTE_MES':
        inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        fim = new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0);
        this.labelFiltroData = 'Este Mês';
        break;
      case 'MES_PASSADO':
        inicio = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1);
        fim = new Date(hoje.getFullYear(), hoje.getMonth(), 0);
        this.labelFiltroData = 'Mês Passado';
        break;
      case 'ESTE_ANO':
        inicio = new Date(hoje.getFullYear(), 0, 1);
        fim = new Date(hoje.getFullYear(), 11, 31);
        this.labelFiltroData = 'Este Ano';
        break;
    }

    this.dataInicio = this.formatarData(inicio);
    this.dataFim = this.formatarData(fim);
    this.aplicarFiltroPersonalizado(this.labelFiltroData);
    
    this.dropdownAberto = false; // 🚀 FECHA O MENU AQUI TAMBÉM
  }

  aplicarFiltroPersonalizado(labelManual?: string) {
    if (!labelManual) {
      this.labelFiltroData = `${this.formatarParaBr(this.dataInicio)} a ${this.formatarParaBr(this.dataFim)}`;
    }
    
    // Agora passamos as datas ao serviço! (Se for TUDO, vai passar strings vazias)
    this.dashboardService.carregarResumo(this.dataInicio, this.dataFim);
    
    this.dropdownAberto = false; // 🚀 FECHA O MENU SE APLICADO MANUALMENTE
  }

  // Helpers de formatação
  private formatarData(data: Date): string {
    const yyyy = data.getFullYear();
    const mm = String(data.getMonth() + 1).padStart(2, '0');
    const dd = String(data.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  private formatarParaBr(dataIso: string): string {
    if (!dataIso) return '';
    const [y, m, d] = dataIso.split('-');
    return `${d}/${m}/${y}`;
  }
}