import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model';

// 🚀 1. A DECLARAÇÃO GLOBAL (Obrigatório para o Bootstrap funcionar como nos Modais)
declare var bootstrap: any;

@Component({
  selector: 'app-conta-corrente-fornecedores',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conta-corrente-fornecedores.html',
  styles: [`
    .card { transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out; }
    .card.border-start:hover { transform: translateY(-3px); box-shadow: 0 .5rem 1.5rem rgba(0,0,0,.08) !important; }
    .table-responsive { max-height: 65vh; overflow-y: auto; }
    .table-responsive::-webkit-scrollbar { width: 6px; }
    .table-responsive::-webkit-scrollbar-thumb { background-color: rgba(0,0,0,.1); border-radius: 10px; }
    .table-responsive thead th { position: sticky; top: 0; background-color: #f8f9fa !important; z-index: 1; box-shadow: inset 0 -1px 0 rgba(0,0,0,.1); }
    .table-hover > tbody > tr { transition: background-color 0.15s ease-in-out; }
    .table-hover > tbody > tr:hover > td { background-color: rgba(13, 110, 253, 0.03) !important; }
    .btn-outline-primary { transition: all 0.2s ease; }
    .btn-outline-primary:hover { transform: scale(1.05); }
  `]
})
export class ContaCorrenteFornecedoresComponent implements OnInit {

  resumoFornecedores: ContaCorrenteResumo[] = [];
  carregando = true;

  totalCompradoGeral = 0;
  totalPagoGeral = 0;
  totalPendenteGeral = 0;

  extratoFornecedor: ContaCorrenteExtrato[] = [];
  carregandoExtrato = false;
  fornecedorSelecionado: ContaCorrenteResumo | null = null;

  constructor(
    private ccService: ContaCorrenteService,
    private cd: ChangeDetectorRef // 🚀 2. INJEÇÃO PARA ACORDAR O ANGULAR
  ) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    this.ccService.obterResumoFornecedores().subscribe({
      next: (dados) => {
        if (dados && dados.length > 0) {
          this.resumoFornecedores = dados.sort((a, b) => b.saldoPendente - a.saldoPendente);
          this.calcularTotais(this.resumoFornecedores);
        } else {
          this.resumoFornecedores = [];
        }
        this.carregando = false;
        this.cd.detectChanges(); // 🚀 Garante que a tabela é desenhada logo
      },
      error: (err) => {
        console.error(err);
        this.carregando = false;
      }
    });
  }

  calcularTotais(lista: ContaCorrenteResumo[]): void {
    this.totalCompradoGeral = lista.reduce((sum, item) => sum + item.totalFaturado, 0);
    this.totalPagoGeral = lista.reduce((sum, item) => sum + item.totalPago, 0);
    this.totalPendenteGeral = lista.reduce((sum, item) => sum + item.saldoPendente, 0);
  }

  abrirExtrato(fornecedor: ContaCorrenteResumo): void {
    this.fornecedorSelecionado = fornecedor;
    this.carregandoExtrato = true;
    this.extratoFornecedor = []; 

    // 🚀 3. SET TIMEOUT PARA NÃO HAVER ATROPELOS ENTRE O CLIQUE E A GAVETA
    setTimeout(() => {
      const offcanvasEl = document.getElementById('offcanvasExtratoFornecedor');
      if (offcanvasEl) {
        let offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
        if (!offcanvas) {
           offcanvas = new bootstrap.Offcanvas(offcanvasEl);
        }
        offcanvas.show();
      }
    }, 0);

    this.ccService.obterExtratoFornecedor(fornecedor.fornecedorId!).subscribe({
      next: (dados) => {
        this.extratoFornecedor = dados;
        this.carregandoExtrato = false;
        this.cd.detectChanges(); // 🚀 Garante que o extrato aparece na gaveta
      },
      error: (err) => {
        console.error(err);
        this.carregandoExtrato = false;
      }
    });
  }
}