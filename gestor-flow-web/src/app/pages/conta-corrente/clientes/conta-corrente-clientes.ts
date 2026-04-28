import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContaCorrenteService } from '../../../services/conta-corrente.service';
import { ContaCorrenteResumo, ContaCorrenteExtrato } from '../../../core/models/conta-corrente.model'; // 🚀 IMPORT ATUALIZADO
import Swal from 'sweetalert2';

declare var bootstrap: any; // 🚀 Permite usar o Javascript do Bootstrap para abrir a gaveta

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

  // Totais Radar
  totalFaturadoGeral = 0;
  totalRecebidoGeral = 0;
  totalPendenteGeral = 0;

  // 🚀 NOVAS VARIÁVEIS PARA O EXTRATO
  extratoCliente: ContaCorrenteExtrato[] = [];
  carregandoExtrato = false;
  clienteSelecionado: ContaCorrenteResumo | null = null;

  constructor(private ccService: ContaCorrenteService) {}

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando = true;
    this.ccService.obterResumoClientes().subscribe({
      next: (dados) => {
        this.resumoClientes = dados.sort((a, b) => b.saldoPendente - a.saldoPendente);
        this.calcularTotais(this.resumoClientes);
        this.carregando = false;
      },
      error: (err) => {
        console.error(err);
        this.carregando = false;
        Swal.fire('Erro de Ligação', 'Não foi possível carregar as contas correntes.', 'error');
      }
    });
  }

  calcularTotais(lista: ContaCorrenteResumo[]): void {
    this.totalFaturadoGeral = lista.reduce((sum, item) => sum + item.totalFaturado, 0);
    this.totalRecebidoGeral = lista.reduce((sum, item) => sum + item.totalPago, 0);
    this.totalPendenteGeral = lista.reduce((sum, item) => sum + item.saldoPendente, 0);
  }

  // 🚀 NOVA FUNÇÃO: Abre a gaveta e vai buscar o histórico ao Java
  abrirExtrato(cliente: ContaCorrenteResumo): void {
    this.clienteSelecionado = cliente;
    this.carregandoExtrato = true;
    this.extratoCliente = []; // Limpa o extrato anterior para não haver "piscar" de dados velhos

    // 1. Abrir a gaveta lateral (Offcanvas)
    const offcanvasEl = document.getElementById('offcanvasExtrato');
    if (offcanvasEl) {
      let offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
      if (!offcanvas) offcanvas = new bootstrap.Offcanvas(offcanvasEl);
      offcanvas.show();
    }

    // 2. Ir ao Backend buscar os dados micro (usando o id do cliente)
    // Nota: Garante que a tua interface ContaCorrenteResumo tem 'clienteId'. 
    this.ccService.obterExtratoCliente(cliente.clienteId!).subscribe({
      next: (dados) => {
        this.extratoCliente = dados;
        this.carregandoExtrato = false;
      },
      error: (err) => {
        console.error(err);
        this.carregandoExtrato = false;
        Swal.fire('Erro', 'Não foi possível carregar o histórico deste cliente.', 'error');
      }
    });
  }
}