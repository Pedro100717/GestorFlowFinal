import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { AnaliticaService } from '../../../services/analitica.service';
import { AnaliseDashboard } from '../../../core/models/analitica.model';

interface GrupoCentroCusto {
  centroCusto: string;
  seccoes: AnaliseDashboard[];
  
  // 📈 Subtotais Operacionais (Tabela 1)
  subtotalVendasSemIva: number;
  subtotalComprasSemIva: number;
  subtotalMargemBruta: number;
  
  // ⚖️ Subtotais Fiscais (Tabela 2)
  subtotalIvaVendas: number;
  subtotalIvaCompras: number;
  subtotalSaldoIva: number;
}

@Component({
  selector: 'app-analise',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './analise.component.html'
})
export class AnaliseComponent implements OnInit {

  // Dados em Memória
  gruposTodos: GrupoCentroCusto[] = [];
  gruposExibidos: GrupoCentroCusto[] = [];
  
  // Filtros
  listaCentros: string[] = [];
  centroSelecionado: string = ''; // Vazio significa "Todos"

  // 📈 Totais Gerais Operacionais
  totalGeralVendasSemIva: number = 0;
  totalGeralComprasSemIva: number = 0;
  totalGeralMargemBruta: number = 0;

  // ⚖️ Totais Gerais Fiscais
  totalGeralIvaVendas: number = 0;
  totalGeralIvaCompras: number = 0;
  totalGeralSaldoIva: number = 0;

  carregando: boolean = true;

  constructor(
    private analiticaService: AnaliticaService,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    this.carregando = true;
    this.analiticaService.obterDashboardAnalitico().subscribe({
      next: (dados) => {
        this.processarDados(dados);
        this.carregando = false;
        this.cd.detectChanges();
      },
      error: (e) => {
        console.error('Erro ao carregar análise:', e);
        this.carregando = false;
      }
    });
  }

  private processarDados(dadosBrutos: AnaliseDashboard[]) {
    const mapa = new Map<string, GrupoCentroCusto>();
    const centrosSet = new Set<string>();

    dadosBrutos.forEach(linha => {
      centrosSet.add(linha.centroCusto);

      if (!mapa.has(linha.centroCusto)) {
        mapa.set(linha.centroCusto, {
          centroCusto: linha.centroCusto,
          seccoes: [],
          
          subtotalVendasSemIva: 0,
          subtotalComprasSemIva: 0,
          subtotalMargemBruta: 0,
          
          subtotalIvaVendas: 0,
          subtotalIvaCompras: 0,
          subtotalSaldoIva: 0
        });
      }

      const grupo = mapa.get(linha.centroCusto)!;
      grupo.seccoes.push(linha);
      
      // Acumular Operacional
      grupo.subtotalVendasSemIva += linha.totalVendasSemIva;
      grupo.subtotalComprasSemIva += linha.totalComprasSemIva;
      grupo.subtotalMargemBruta += linha.margemBruta;
      
      // Acumular Fiscal
      grupo.subtotalIvaVendas += linha.totalIvaVendas;
      grupo.subtotalIvaCompras += linha.totalIvaCompras;
      grupo.subtotalSaldoIva += linha.saldoIva;
    });

    this.gruposTodos = Array.from(mapa.values());
    this.listaCentros = Array.from(centrosSet).sort();

    this.aplicarFiltro();
  }

  aplicarFiltro() {
    if (this.centroSelecionado === '') {
      this.gruposExibidos = [...this.gruposTodos];
    } else {
      this.gruposExibidos = this.gruposTodos.filter(g => g.centroCusto === this.centroSelecionado);
    }
    
    this.recalcularTotais();
  }

  private recalcularTotais() {
    // Reset Operacional
    this.totalGeralVendasSemIva = 0;
    this.totalGeralComprasSemIva = 0;
    this.totalGeralMargemBruta = 0;
    
    // Reset Fiscal
    this.totalGeralIvaVendas = 0;
    this.totalGeralIvaCompras = 0;
    this.totalGeralSaldoIva = 0;

    this.gruposExibidos.forEach(g => {
      // Somar Operacional
      this.totalGeralVendasSemIva += g.subtotalVendasSemIva;
      this.totalGeralComprasSemIva += g.subtotalComprasSemIva;
      this.totalGeralMargemBruta += g.subtotalMargemBruta;
      
      // Somar Fiscal
      this.totalGeralIvaVendas += g.subtotalIvaVendas;
      this.totalGeralIvaCompras += g.subtotalIvaCompras;
      this.totalGeralSaldoIva += g.subtotalSaldoIva;
    });
  }
}