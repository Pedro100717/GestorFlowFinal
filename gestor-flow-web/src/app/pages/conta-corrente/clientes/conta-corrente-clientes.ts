import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 1. IMPORT OBRIGATÓRIO DOS ERROS HTTP
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model';
import { LogService } from '../../../core/services/log.service'; // 🚀 2. INJEÇÃO DO NOSSO INSPETOR
import Swal from 'sweetalert2';

// 🚀 DECLARAÇÃO GLOBAL
declare var bootstrap: any;

@Component({
  selector: 'app-conta-corrente-clientes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conta-corrente-clientes.html',
  styleUrl: './conta-corrente-clientes.scss'
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
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 3. SERVIÇO DECLARADO
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
        this.logService.debug('Resumo de contas correntes de clientes carregado com sucesso.', this.resumoClientes.length); // 🚀 RASTREABILIDADE
        this.carregando = false;
        this.cd.detectChanges();
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error('Erro ao carregar o resumo das contas correntes dos clientes', err); // 🚀 CAIXA NEGRA
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

    // SET TIMEOUT PARA EVITAR ATROPELAMENTOS NO DOM
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
        this.logService.debug(`Extrato carregado com sucesso para o cliente ${cliente.clienteId}`); // 🚀 RASTREABILIDADE
        this.carregandoExtrato = false;
        this.cd.detectChanges(); 
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error(`Erro ao carregar extrato do cliente ${cliente.clienteId}`, err); // 🚀 CAIXA NEGRA
        this.carregandoExtrato = false;
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível carregar o extrato do cliente selecionado.', 'error');
      }
    });
  }

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
        this.logService.info(`PDF de Conta Corrente exportado para o cliente ${this.clienteSelecionado?.clienteId}`); // 🚀 RASTREABILIDADE
        this.carregandoExtrato = false;
        this.cd.detectChanges();
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error(`Erro ao exportar PDF do extrato do cliente ${this.clienteSelecionado?.clienteId}`, err); // 🚀 CAIXA NEGRA
        this.carregandoExtrato = false;
        this.cd.detectChanges();
        Swal.fire('Erro', 'Não foi possível gerar o extrato em PDF. Verifica o estado do servidor.', 'error');
      }
    });
  }
}