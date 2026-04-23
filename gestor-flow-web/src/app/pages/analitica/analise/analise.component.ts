import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // 🛡️ IMPORTANTE: Adicionado para o ngModel funcionar
import { AnaliticaService } from '../../../services/analitica.service';
import { AnaliseDashboard } from '../../../core/models/analitica.model';

interface GrupoCentroCusto {
  centroCusto: string;
  seccoes: AnaliseDashboard[];
  subtotalVendas: number;
  subtotalCompras: number;
  subtotalMargem: number;
}

@Component({
  selector: 'app-analise',
  standalone: true,
  imports: [CommonModule, FormsModule], // 🛡️ FormsModule ADICIONADO AQUI
  templateUrl: './analise.component.html'
})
export class AnaliseComponent implements OnInit {

  // Dados em Memória
  gruposTodos: GrupoCentroCusto[] = [];
  gruposExibidos: GrupoCentroCusto[] = [];
  
  // Filtros
  listaCentros: string[] = [];
  centroSelecionado: string = ''; // Vazio significa "Todos"

  // Totais Visíveis
  totalGeralVendas: number = 0;
  totalGeralCompras: number = 0;
  totalGeralMargem: number = 0;

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
    const centrosSet = new Set<string>(); // Para extrair os nomes únicos dos centros

    dadosBrutos.forEach(linha => {
      centrosSet.add(linha.centroCusto);

      if (!mapa.has(linha.centroCusto)) {
        mapa.set(linha.centroCusto, {
          centroCusto: linha.centroCusto,
          seccoes: [],
          subtotalVendas: 0,
          subtotalCompras: 0,
          subtotalMargem: 0
        });
      }

      const grupo = mapa.get(linha.centroCusto)!;
      grupo.seccoes.push(linha);
      grupo.subtotalVendas += linha.totalVendas;
      grupo.subtotalCompras += linha.totalCompras;
      grupo.subtotalMargem += linha.margem;
    });

    // Guarda tudo na "Gaveta Principal"
    this.gruposTodos = Array.from(mapa.values());
    
    // Preenche as opções do Dropdown
    this.listaCentros = Array.from(centrosSet).sort();

    // Aplica o filtro atual (por defeito mostra "Todos")
    this.aplicarFiltro();
  }

  // 🚀 O MOTOR DE FILTRAGEM INSTANTÂNEA
  aplicarFiltro() {
    if (this.centroSelecionado === '') {
      // Se não houver filtro, exibe todos
      this.gruposExibidos = [...this.gruposTodos];
    } else {
      // Se houver filtro, exibe só o selecionado
      this.gruposExibidos = this.gruposTodos.filter(g => g.centroCusto === this.centroSelecionado);
    }
    
    this.recalcularTotais();
  }

  private recalcularTotais() {
    this.totalGeralVendas = 0;
    this.totalGeralCompras = 0;
    this.totalGeralMargem = 0;

    // Soma apenas os valores que estão visíveis no ecrã!
    this.gruposExibidos.forEach(g => {
      this.totalGeralVendas += g.subtotalVendas;
      this.totalGeralCompras += g.subtotalCompras;
      this.totalGeralMargem += g.subtotalMargem;
    });
  }
}