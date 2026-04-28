import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model';
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-conta-corrente-fornecedores',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conta-corrente-fornecedores.html',
  // Truque de Mestre: Reaproveitamos o ficheiro CSS dos clientes para não duplicar código!
  styleUrls: ['../clientes/conta-corrente-clientes.scss'] 
})
export class ContaCorrenteFornecedoresComponent implements OnInit {

  resumoFornecedores: ContaCorrenteResumo[] = [];
  carregando = true;

  // Totais Radar
  totalCompradoGeral = 0;
  totalPagoGeral = 0;
  totalPendenteGeral = 0;

  // Variáveis do Extrato (Visão Micro)
  extratoFornecedor: ContaCorrenteExtrato[] = [];
  carregandoExtrato = false;
  fornecedorSelecionado: ContaCorrenteResumo | null = null;

  constructor(private ccService: ContaCorrenteService) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    this.ccService.obterResumoFornecedores().subscribe({
      next: (dados) => {
        // Ordenamos dos que devemos MAIS para os que devemos MENOS
        this.resumoFornecedores = dados.sort((a, b) => b.saldoPendente - a.saldoPendente);
        this.calcularTotais(this.resumoFornecedores);
        this.carregando = false;
      },
      error: (err) => {
        console.error(err);
        this.carregando = false;
        Swal.fire('Erro de Ligação', 'Não foi possível carregar as contas de fornecedores.', 'error');
      }
    });
  }

  calcularTotais(lista: ContaCorrenteResumo[]): void {
    // Reutilizamos as propriedades do modelo (totalFaturado = Comprado, totalPago = Pago na ótica do fornecedor)
    this.totalCompradoGeral = lista.reduce((sum, item) => sum + item.totalFaturado, 0);
    this.totalPagoGeral = lista.reduce((sum, item) => sum + item.totalPago, 0);
    this.totalPendenteGeral = lista.reduce((sum, item) => sum + item.saldoPendente, 0);
  }

  abrirExtrato(fornecedor: ContaCorrenteResumo): void {
    this.fornecedorSelecionado = fornecedor;
    this.carregandoExtrato = true;
    this.extratoFornecedor = []; 

    const offcanvasEl = document.getElementById('offcanvasExtratoFornecedor');
    if (offcanvasEl) {
      let offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
      if (!offcanvas) offcanvas = new bootstrap.Offcanvas(offcanvasEl);
      offcanvas.show();
    }

    // Passamos o fornecedorId em vez do clienteId
    this.ccService.obterExtratoFornecedor(fornecedor.fornecedorId!).subscribe({
      next: (dados) => {
        this.extratoFornecedor = dados;
        this.carregandoExtrato = false;
      },
      error: (err) => {
        console.error(err);
        this.carregandoExtrato = false;
        Swal.fire('Erro', 'Não foi possível carregar o histórico deste fornecedor.', 'error');
      }
    });
  }
}