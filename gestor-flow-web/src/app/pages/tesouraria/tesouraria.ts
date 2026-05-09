import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { TesourariaService } from '../../services/tesouraria.service';
import { ClienteService } from '../../services/cliente.service'; 
import { FornecedorService } from '../../services/fornecedor.service';

import { ContaBancaria, Movimento, DocumentoPendente, SimuladorTesourariaDTO } from '../../core/models/tesouraria.model'; 
import { Cliente } from '../../core/models/cliente.model';
import { Fornecedor } from '../../core/models/fornecedor.model';

import Swal from 'sweetalert2';

// 🚀 IMPORT DO MOTOR GRÁFICO
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables); 

declare var bootstrap: any;

@Component({
  selector: 'app-tesouraria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tesouraria.html'
})
export class TesourariaComponent implements OnInit, AfterViewInit {

  abaAtiva: 'contas' | 'pendentes' | 'simulador' = 'contas'; 

  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;

  listaPendentes: DocumentoPendente[] = []; 
  docParaConfirmar: DocumentoPendente | null = null; 

  clientes: Cliente[] = [];
  fornecedores: Fornecedor[] = [];

  formConta!: FormGroup;
  formMovimento!: FormGroup;
  formTransferencia!: FormGroup;
  formConfirmacao!: FormGroup; 

  // 🚀 VARIÁVEIS DO GRÁFICO
  simulacaoAtual: SimuladorTesourariaDTO | null = null;
  chartInstance: any;

  // 🚀 VARIÁVEIS DA TABELA DESENHADA À MÃO
  linhasSimulador: any[] = [];
  saldoAtualTotal: number = 0;

  constructor(
    private tesourariaService: TesourariaService,
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormularios();

    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      if (this.contaSelecionada) {
        this.contaSelecionada = contas.find(c => c.id === this.contaSelecionada!.id) || null;
      }
      this.gerarTabelaSimulador(); // Atualiza a tabela manual se o saldo mudar
      this.cd.detectChanges();
    });

    this.tesourariaService.movimentos$.subscribe(movs => {
      this.movimentos = movs;
      this.cd.detectChanges();
    });

    this.carregarDados();
    this.carregarEntidades();
  }

  ngAfterViewInit() {
    this.carregarSimulador();
  }

  carregarDados() {
    this.tesourariaService.carregarContasDaAPI();
    this.carregarPendentes();
  }

  carregarEntidades() {
    this.clienteService.listar().subscribe((res: any) => {
      this.clientes = res.content || res;
    });
    
    this.fornecedorService.listar().subscribe((res: any) => {
      this.fornecedores = res.content || res;
    });
  }

  carregarPendentes() {
    this.tesourariaService.listarPendentes().subscribe({
      next: (dados: DocumentoPendente[]) => {
        this.listaPendentes = dados;
        this.gerarTabelaSimulador(); // Atualiza a tabela manual se entrarem pendentes
        this.cd.detectChanges();
      },
      error: (e: HttpErrorResponse) => console.error('Erro ao carregar pendentes:', e.message)
    });
  }

  // =========================================================================
  // --- 🚀 CONSTRUTOR DA TABELA DESENHADA PELO USER ---
  // =========================================================================
  gerarTabelaSimulador() {
    this.saldoAtualTotal = this.listaContas.reduce((acc, c) => acc + c.saldo, 0);
    this.linhasSimulador = [];

    this.linhasSimulador.push({
      isAtual: true,
      descritivo: 'SALDO ATUAL',
      saldo: this.saldoAtualTotal
    });

    const pendentesOrdenados = [...this.listaPendentes].sort((a, b) => new Date(a.data).getTime() - new Date(b.data).getTime());

    let saldoCorrente = this.saldoAtualTotal;
    
    pendentesOrdenados.forEach(doc => {
      let receita = doc.tipo === 'VENDA' ? doc.valorPendente : null;
      let despesa = doc.tipo === 'COMPRA' ? doc.valorPendente : null;
      
      if (receita) saldoCorrente += receita;
      if (despesa) saldoCorrente -= despesa;

      this.linhasSimulador.push({
        isAtual: false,
        data: doc.data,
        descritivo: doc.entidade,
        receita: receita,
        despesa: despesa,
        saldo: saldoCorrente,
        documento: doc 
      });
    });
  }

  // =========================================================================
  // --- 🚀 O MOTOR GRÁFICO (SIMULADOR) ---
  // =========================================================================
  
  atualizarGraficoSimulador() {
    setTimeout(() => {
        this.carregarSimulador();
    }, 100);
  }

  carregarSimulador() {
    this.tesourariaService.obterSimulacao().subscribe({
      next: (dados: SimuladorTesourariaDTO) => {
        this.simulacaoAtual = dados;
        this.desenharGrafico(dados);
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro a carregar simulador:', e)
    });
  }

  desenharGrafico(dados: SimuladorTesourariaDTO) {
    const canvas = document.getElementById('graficoSimulador') as HTMLCanvasElement;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.chartInstance) {
      this.chartInstance.destroy();
    }

    const labels = dados.pontos.map(p => p.label);
    
    // 🚀 LIMPO E ESTRUTURADO: Lê diretamente do modelo tipado (Sem maroscas)!
    const saldos = dados.pontos.map(p => p.saldoProjetado || 0);

    const riscoTesouraria = saldos.some(s => s < 0);
    const corLinha = riscoTesouraria ? '#dc3545' : '#0d6efd'; 
    const corFundo = riscoTesouraria ? 'rgba(220, 53, 69, 0.1)' : 'rgba(13, 110, 253, 0.1)';

    this.chartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Saldo Projetado (€)',
          data: saldos,
          borderColor: corLinha,
          backgroundColor: corFundo,
          borderWidth: 3,
          tension: 0.4,
          fill: true,
          pointBackgroundColor: corLinha,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (context: any) => ` ${context.parsed.y.toLocaleString('pt-PT', {style: 'currency', currency: 'EUR'})}`
            }
          }
        },
        scales: {
          y: { 
            beginAtZero: false,
            grid: { color: '#e9ecef' }
          },
          x: { 
            grid: { display: false }
          }
        }
      }
    });
  }

  // =========================================================================
  // --- INICIALIZAÇÕES E UTILITÁRIOS ---
  // =========================================================================

  inicializarFormularios() {
    this.formConta = this.fb.group({
      nome: ['', Validators.required],
      iban: [''], 
      saldoInicial: [0]
    });

    this.formMovimento = this.fb.group({
      contaId: [null, Validators.required],
      descricao: ['', Validators.required],
      tipo: ['DEBITO', Validators.required],
      valor: [0, [Validators.required, Validators.min(0.01)]],
      clienteId: [null],
      fornecedorId: [null]
    });

    this.configurarObrigatoriedadeDinamica();

    this.formTransferencia = this.fb.group({
      contaOrigemId: [null, Validators.required],
      contaDestinoId: [null, Validators.required],
      valor: [0, [Validators.required, Validators.min(0.01)]],
      descricao: ['']
    });

    this.formConfirmacao = this.fb.group({
      contaBancariaId: [null, Validators.required],
      dataPagamento: [this.getDataAtual(), Validators.required],
      valorAPagar: [null, [Validators.required, Validators.min(0.01)]]
    });
  }

  configurarObrigatoriedadeDinamica() {
    const tipoControl = this.formMovimento.get('tipo');
    const clienteControl = this.formMovimento.get('clienteId');
    const fornecedorControl = this.formMovimento.get('fornecedorId');

    this.aplicarRegras(tipoControl?.value, clienteControl, fornecedorControl);

    tipoControl?.valueChanges.subscribe(tipoSelecionado => {
      this.aplicarRegras(tipoSelecionado, clienteControl, fornecedorControl);
    });
  }

  private aplicarRegras(tipoSelecionado: string, clienteControl: AbstractControl | null, fornecedorControl: AbstractControl | null) {
    if (!clienteControl || !fornecedorControl) return;

    if (tipoSelecionado === 'DEBITO' || tipoSelecionado === 'SAIDA') {
      fornecedorControl.setValidators([Validators.required]);
      clienteControl.clearValidators();
      clienteControl.setValue(null);
    } else if (tipoSelecionado === 'CREDITO' || tipoSelecionado === 'ENTRADA') {
      clienteControl.setValidators([Validators.required]);
      fornecedorControl.clearValidators();
      fornecedorControl.setValue(null);
    } else {
      clienteControl.clearValidators();
      fornecedorControl.clearValidators();
    }
    clienteControl.updateValueAndValidity();
    fornecedorControl.updateValueAndValidity();
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  // =========================================================================
  // --- ACÇÕES FINANCEIRAS ---
  // =========================================================================

  abrirModalConfirmacao(doc: DocumentoPendente) {
    this.docParaConfirmar = doc;
    const contaPadrao = this.listaContas.length > 0 ? this.listaContas[0].id : null;
    this.formConfirmacao.reset({ 
      dataPagamento: this.getDataAtual(), 
      contaBancariaId: contaPadrao,
      valorAPagar: doc.valorPendente
    });
    new bootstrap.Modal(document.getElementById('modalConfirmacao')).show();
  }

  confirmarTransacao() {
    if (this.formConfirmacao.invalid || !this.docParaConfirmar) {
      this.formConfirmacao.markAllAsTouched();
      return;
    }

    const payload = {
      documentoId: this.docParaConfirmar.id,
      tipoDocumento: this.docParaConfirmar.tipo,
      contaBancariaId: this.formConfirmacao.value.contaBancariaId,
      dataPagamento: this.formConfirmacao.value.dataPagamento,
      valorAPagar: this.formConfirmacao.value.valorAPagar
    };

    this.tesourariaService.confirmarTransacao(payload).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Transação liquidada com sucesso!' });
        bootstrap.Modal.getInstance(document.getElementById('modalConfirmacao'))?.hide();
        
        this.carregarPendentes();
        this.tesourariaService.carregarContasDaAPI(); 
        if (this.abaAtiva === 'simulador') this.carregarSimulador(); 
      },
      error: (e: HttpErrorResponse) => {
        Swal.fire({ icon: 'error', title: 'Falha ao Liquidar', text: e.error?.message || 'Ocorreu um erro.', confirmButtonColor: '#0d6efd' });
      }
    });
  }

  novaConta() {
    this.formConta.reset({ saldoInicial: 0, iban: 'PT50' }); 
    new bootstrap.Modal(document.getElementById('modalConta')).show();
  }

  guardarConta() {
    if (this.formConta.invalid) {
      this.formConta.markAllAsTouched();
      return;
    }
    this.tesourariaService.criarConta(this.formConta.value).subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Conta criada com sucesso!' });
        bootstrap.Modal.getInstance(document.getElementById('modalConta'))?.hide();
        if (this.abaAtiva === 'simulador') this.carregarSimulador(); 
      }
    });
  }

  novoMovimento() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formMovimento.reset({ contaId: contaId, tipo: 'DEBITO', valor: 0, descricao: '', clienteId: null, fornecedorId: null });
    this.aplicarRegras('DEBITO', this.formMovimento.get('clienteId'), this.formMovimento.get('fornecedorId'));
    new bootstrap.Modal(document.getElementById('modalMovimento')).show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) {
      this.formMovimento.markAllAsTouched();
      return;
    }
    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Movimento registado!' });
        bootstrap.Modal.getInstance(document.getElementById('modalMovimento'))?.hide();
        if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
        if (this.abaAtiva === 'simulador') this.carregarSimulador(); 
      }
    });
  }

  verExtrato(conta: ContaBancaria) {
    this.contaSelecionada = conta;
    this.tesourariaService.obterExtrato(conta.id!);
  }

  novaTransferencia() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formTransferencia.reset({ contaOrigemId: contaId, contaDestinoId: null, valor: 0, descricao: '' });
    new bootstrap.Modal(document.getElementById('modalTransferencia')).show();
  }

  realizarTransferencia() {
    if (this.formTransferencia.invalid) {
      this.formTransferencia.markAllAsTouched();
      return;
    }
    const origem = this.formTransferencia.get('contaOrigemId')?.value;
    const destino = this.formTransferencia.get('contaDestinoId')?.value;
    if (origem === destino) {
      Swal.fire({ icon: 'warning', title: 'Operação Inválida', text: 'A conta origem e destino não podem ser a mesma.' });
      return;
    }
    this.tesourariaService.realizarTransferencia(this.formTransferencia.value).subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Transferência realizada!' });
        bootstrap.Modal.getInstance(document.getElementById('modalTransferencia'))?.hide();
        if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
      }
    });
  }

  anularMovimento(mov: Movimento) {
    Swal.fire({
      title: 'Anular Movimento?',
      text: `Vai anular a transação de ${mov.valor}€. O saldo será revertido.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      confirmButtonText: 'Sim, anular e reverter!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.tesourariaService.anularMovimento(mov.id!).subscribe({
          next: () => {
            Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Estornado!' });
            if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
            this.carregarPendentes();
            if (this.abaAtiva === 'simulador') this.carregarSimulador(); 
          }
        });
      }
    });
  }
}