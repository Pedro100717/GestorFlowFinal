import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model';
import Swal from 'sweetalert2'; // 🚀 Importado para manter o padrão de alertas dos clientes

// 🚀 1. A DECLARAÇÃO GLOBAL (Obrigatório para o Bootstrap funcionar como nos Modais)
declare var bootstrap: any;

@Component({
  selector: 'app-conta-corrente-fornecedores',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conta-corrente-fornecedores.html',
  styleUrl: './conta-corrente-fornecedores.scss'
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
        Swal.fire('Erro', 'Não foi possível carregar os resumos dos fornecedores.', 'error');
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
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível carregar o extrato do fornecedor.', 'error');
      }
    });
  }

  // 🚀 4. NOVO MÉTODO PARA EXPORTAR O EXTRATO DO FORNECEDOR EM PDF
  exportarExtratoPdf(): void {
    if (!this.fornecedorSelecionado || !this.fornecedorSelecionado.fornecedorId) {
      Swal.fire('Aviso', 'Nenhum fornecedor selecionado para exportação.', 'warning');
      return;
    }

    this.carregandoExtrato = true;
    this.cd.detectChanges();

    this.ccService.extrairPdfExtratoFornecedor(this.fornecedorSelecionado.fornecedorId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        this.carregandoExtrato = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao exportar PDF do fornecedor:', err);
        this.carregandoExtrato = false;
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível gerar o extrato em PDF para este fornecedor.', 'error');
      }
    });
  }
}