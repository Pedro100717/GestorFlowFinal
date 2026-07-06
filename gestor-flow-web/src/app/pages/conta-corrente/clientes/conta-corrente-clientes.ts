import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model';
import Swal from 'sweetalert2';

// 🚀 DECLARAÇÃO GLOBAL (Obrigatório, igual aos Centros de Custo)
declare var bootstrap: any;

@Component({
  selector: 'app-conta-corrente-clientes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conta-corrente-clientes.html',
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
export class ContaCorrenteClientesComponent implements OnInit {

  resumoClientes: ContaCorrenteResumo[] = [];
  carregando = true;

  totalFaturadoGeral = 0;
  totalRecebidoGeral = 0;
  totalPendenteGeral = 0;

  extratoCliente: ContaCorrenteExtrato[] = [];
  carregandoExtrato = false;
  clienteSelecionado: ContaCorrenteResumo | null = null;

  constructor(
    private ccService: ContaCorrenteService,
    private cd: ChangeDetectorRef // 🚀 INJEÇÃO PARA ACORDAR O ANGULAR
  ) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    this.ccService.obterResumoClientes().subscribe({
      next: (dados) => {
        if (dados && dados.length > 0) {
          this.resumoClientes = dados.sort((a, b) => b.saldoPendente - a.saldoPendente);
          this.calcularTotais(this.resumoClientes);
        } else {
          this.resumoClientes = [];
        }
        this.carregando = false;
        this.cd.detectChanges(); // Atualiza a vista
      },
      error: (err) => {
        console.error(err);
        this.carregando = false;
        Swal.fire('Erro', 'Não foi possível carregar os dados de resumo dos clientes.', 'error');
      }
    });
  }

  calcularTotais(lista: ContaCorrenteResumo[]): void {
    this.totalFaturadoGeral = lista.reduce((sum, item) => sum + item.totalFaturado, 0);
    this.totalRecebidoGeral = lista.reduce((sum, item) => sum + item.totalPago, 0);
    this.totalPendenteGeral = lista.reduce((sum, item) => sum + item.saldoPendente, 0);
  }

  abrirExtrato(cliente: ContaCorrenteResumo): void {
    this.clienteSelecionado = cliente;
    this.carregandoExtrato = true;
    this.extratoCliente = []; 

    // 🚀 SET TIMEOUT PARA EVITAR ATROPELAMENTOS NO DOM
    setTimeout(() => {
      const offcanvasEl = document.getElementById('offcanvasExtrato');
      if (offcanvasEl) {
        let offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
        if (!offcanvas) offcanvas = new bootstrap.Offcanvas(offcanvasEl);
        offcanvas.show();
      }
    }, 0);

    this.ccService.obterExtratoCliente(cliente.clienteId!).subscribe({
      next: (dados) => {
        this.extratoCliente = dados;
        this.carregandoExtrato = false;
        this.cd.detectChanges(); // Atualiza a gaveta com os dados
      },
      error: (err) => {
        console.error(err);
        this.carregandoExtrato = false;
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível carregar o extrato do cliente selecionado.', 'error');
      }
    });
  }

  // 🚀 MÉTODO ATUALIZADO PARA EXPORTAR O EXTRATO EM PDF
  exportarExtratoPdf(): void {
    if (!this.clienteSelecionado || !this.clienteSelecionado.clienteId) {
      Swal.fire('Aviso', 'Nenhum cliente selecionado para exportação.', 'warning');
      return;
    }

    this.carregandoExtrato = true; 
    this.cd.detectChanges();

    this.ccService.extrairPdfExtratoCliente(this.clienteSelecionado.clienteId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        this.carregandoExtrato = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao exportar PDF:', err);
        this.carregandoExtrato = false;
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível gerar o extrato em PDF. Verifica o estado do servidor.', 'error');
      }
    });
  }
}