import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardService } from '../../services/dashboard.service';
import { VendaResumo } from '../../core/models/dashboard.model';
import { LogService } from '../../core/services/log.service'; // 🚀 1. IMPORT DO INSPETOR

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html'
})
export class DashboardComponent implements OnInit {

  dataHoje: Date = new Date();
  
  // Controla o estado do menu (aberto/fechado)
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
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 2. INJEÇÃO DO LOG SERVICE
  ) {}

  ngOnInit() {
    this.dashboardService.resumo$.subscribe(dados => {
      if (dados) {
        this.totalVendas = dados.totalVendas || 0;
        this.valorStock = dados.valorStock || 0;
        this.margemBruta = dados.margemBruta || 0;
        this.totalCompras = dados.totalCompras || 0;
        this.ultimasVendas = dados.ultimasVendas || [];
        
        // 🚀 RASTREABILIDADE SILENCIOSA: Sabemos sempre quando o ecrã atualizou os números
        this.logService.debug('Painel de Dashboard renderizado com novos dados.'); 
        
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
      
      this.logService.info('Filtro de Dashboard aplicado: Todo o Histórico'); // 🚀 CAIXA NEGRA
      this.aplicarFiltroPersonalizado(this.labelFiltroData);
      
      this.dropdownAberto = false; 
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
    
    this.logService.info(`Filtro Rápido de Dashboard aplicado: ${this.labelFiltroData}`); // 🚀 CAIXA NEGRA
    this.aplicarFiltroPersonalizado(this.labelFiltroData);
    
    this.dropdownAberto = false; 
  }

  aplicarFiltroPersonalizado(labelManual?: string) {
    if (!labelManual) {
      this.labelFiltroData = `${this.formatarParaBr(this.dataInicio)} a ${this.formatarParaBr(this.dataFim)}`;
      this.logService.info(`Filtro Manual de Dashboard aplicado: ${this.dataInicio} até ${this.dataFim}`); // 🚀 CAIXA NEGRA
    }
    
    // Agora passamos as datas ao serviço! (Se for TUDO, vai passar strings vazias)
    this.dashboardService.carregarResumo(this.dataInicio, this.dataFim);
    
    this.dropdownAberto = false; 
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

  abrirPainelFiltro() {
    this.dropdownAberto = false; 
    
    setTimeout(() => {
      const offcanvasElement = document.getElementById('offcanvasFiltroDatas');
      if (offcanvasElement && (window as any).bootstrap) {
        const bsOffcanvas = (window as any).bootstrap.Offcanvas.getInstance(offcanvasElement) || new (window as any).bootstrap.Offcanvas(offcanvasElement);
        bsOffcanvas.show();
      }
    }, 100);
  }
}