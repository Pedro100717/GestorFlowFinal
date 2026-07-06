import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { AnaliticaService } from '../../../services/analitica.service';
import { AnaliseDashboard } from '../../../core/models/analitica.model';

interface GrupoCentroCusto {
  centroCustoCodigo: string;
  centroCustoNome: string;
  seccoes: AnaliseDashboard[];
  
  subtotalVendasSemIva: number;
  subtotalComprasSemIva: number;
  subtotalMargemBruta: number;
  
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

  gruposTodos: GrupoCentroCusto[] = [];
  gruposExibidos: GrupoCentroCusto[] = [];
  
  // 🚀 Filtros Duplos
  listaCentros: { codigo: string, nome: string }[] = [];
  centroSelecionado: string = ''; 

  listaSeccoes: { codigo: string, nome: string }[] = [];
  seccaoSelecionada: string = ''; 

  totalGeralVendasSemIva: number = 0;
  totalGeralComprasSemIva: number = 0;
  totalGeralMargemBruta: number = 0;

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
    const centrosMap = new Map<string, string>();
    const seccoesMap = new Map<string, string>();

    dadosBrutos.forEach(linha => {
      // Regista os nomes para os filtros
      if(!centrosMap.has(linha.centroCustoCodigo)) centrosMap.set(linha.centroCustoCodigo, linha.centroCustoNome);
      if(!seccoesMap.has(linha.seccaoCodigo)) seccoesMap.set(linha.seccaoCodigo, linha.seccaoNome);

      if (!mapa.has(linha.centroCustoCodigo)) {
        mapa.set(linha.centroCustoCodigo, {
          centroCustoCodigo: linha.centroCustoCodigo,
          centroCustoNome: linha.centroCustoNome,
          seccoes: [],
          subtotalVendasSemIva: 0, subtotalComprasSemIva: 0, subtotalMargemBruta: 0,
          subtotalIvaVendas: 0, subtotalIvaCompras: 0, subtotalSaldoIva: 0
        });
      }

      const grupo = mapa.get(linha.centroCustoCodigo)!;
      grupo.seccoes.push(linha);
      
      // Totais Base
      grupo.subtotalVendasSemIva += linha.totalVendasSemIva || 0;
      grupo.subtotalComprasSemIva += linha.totalComprasSemIva || 0;
      grupo.subtotalMargemBruta += linha.margemBruta || 0;
      grupo.subtotalIvaVendas += linha.totalIvaVendas || 0;
      grupo.subtotalIvaCompras += linha.totalIvaCompras || 0;
      grupo.subtotalSaldoIva += linha.saldoIva || 0;
    });

    this.gruposTodos = Array.from(mapa.values());
    
    this.listaCentros = Array.from(centrosMap.entries()).map(([codigo, nome]) => ({ codigo, nome })).sort((a, b) => a.nome.localeCompare(b.nome));
    this.listaSeccoes = Array.from(seccoesMap.entries()).map(([codigo, nome]) => ({ codigo, nome })).sort((a, b) => a.nome.localeCompare(b.nome));

    this.aplicarFiltro();
  }

  aplicarFiltro() {
    this.gruposExibidos = [];

    this.gruposTodos.forEach(g => {
      // 1. Validar Filtro do Centro de Custo
      if (this.centroSelecionado !== '' && g.centroCustoCodigo !== this.centroSelecionado) return;

      // 2. Validar Filtro da Secção (e extrair apenas as que interessam)
      const seccoesFiltradas = this.seccaoSelecionada === '' 
        ? g.seccoes 
        : g.seccoes.filter(s => s.seccaoCodigo === this.seccaoSelecionada);

      // 3. Se sobrarem secções, recalcula o subtotal do grupo e adiciona
      if (seccoesFiltradas.length > 0) {
        this.gruposExibidos.push(this.clonarERecalcularGrupo(g, seccoesFiltradas));
      }
    });
    
    this.recalcularTotais();
  }

  // 🚀 O MOTOR MATEMÁTICO: Recalcula subtotais se uma secção for escondida
  private clonarERecalcularGrupo(grupoOriginal: GrupoCentroCusto, seccoes: AnaliseDashboard[]): GrupoCentroCusto {
    return {
      centroCustoCodigo: grupoOriginal.centroCustoCodigo,
      centroCustoNome: grupoOriginal.centroCustoNome,
      seccoes: seccoes,
      subtotalVendasSemIva: seccoes.reduce((sum, s) => sum + (s.totalVendasSemIva || 0), 0),
      subtotalComprasSemIva: seccoes.reduce((sum, s) => sum + (s.totalComprasSemIva || 0), 0),
      subtotalMargemBruta: seccoes.reduce((sum, s) => sum + (s.margemBruta || 0), 0),
      subtotalIvaVendas: seccoes.reduce((sum, s) => sum + (s.totalIvaVendas || 0), 0),
      subtotalIvaCompras: seccoes.reduce((sum, s) => sum + (s.totalIvaCompras || 0), 0),
      subtotalSaldoIva: seccoes.reduce((sum, s) => sum + (s.saldoIva || 0), 0)
    };
  }

  private recalcularTotais() {
    this.totalGeralVendasSemIva = 0; this.totalGeralComprasSemIva = 0; this.totalGeralMargemBruta = 0;
    this.totalGeralIvaVendas = 0; this.totalGeralIvaCompras = 0; this.totalGeralSaldoIva = 0;

    this.gruposExibidos.forEach(g => {
      this.totalGeralVendasSemIva += g.subtotalVendasSemIva;
      this.totalGeralComprasSemIva += g.subtotalComprasSemIva;
      this.totalGeralMargemBruta += g.subtotalMargemBruta;
      this.totalGeralIvaVendas += g.subtotalIvaVendas;
      this.totalGeralIvaCompras += g.subtotalIvaCompras;
      this.totalGeralSaldoIva += g.subtotalSaldoIva;
    });
  }

  gerarPdfDashboard() {
    this.carregando = true; // Feedback visual opcional
    this.analiticaService.extrairPdfDashboard().subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        this.carregando = false;
      },
      error: (err) => {
        console.error('Erro na exportação:', err);
        this.carregando = false;
        // Se usares SweetAlert, podes adicionar aqui:
        // Swal.fire('Erro', 'Não foi possível gerar o PDF.', 'error');
      }
    });
  }
}