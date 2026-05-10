import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { TesourariaService } from '../../services/tesouraria.service';
import { ClienteService } from '../../services/cliente.service'; 
import { FornecedorService } from '../../services/fornecedor.service';
import { PlaneamentoService } from '../../services/planeamento.service';

// 🚀 IMPORT CORRETO (Tudo vem do AnaliticaService)
import { AnaliticaService } from '../../services/analitica.service';

import { 
  ContaBancaria, Movimento, DocumentoPendente, 
  SimuladorTesourariaDTO, MovimentoPlaneado, 
  TipoMovimentoPlaneado, FrequenciaMovimento 
} from '../../core/models/tesouraria.model'; 
import { Cliente } from '../../core/models/cliente.model';
import { Fornecedor } from '../../core/models/fornecedor.model';

import Swal from 'sweetalert2';
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

  // --- DADOS REAIS ---
  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;
  listaPendentes: DocumentoPendente[] = []; 
  docParaConfirmar: DocumentoPendente | null = null; 

  // --- DADOS DE PLANEAMENTO & ANALÍTICA ---
  listaPlanos: MovimentoPlaneado[] = [];
  clientes: Cliente[] = [];
  fornecedores: Fornecedor[] = [];
  centrosCusto: any[] = [];
  todasSeccoes: any[] = []; 

  // --- FORMULÁRIOS ---
  formConta!: FormGroup;
  formMovimento!: FormGroup;
  formTransferencia!: FormGroup;
  formConfirmacao!: FormGroup; 
  formPlaneamento!: FormGroup; 

  // --- SIMULADOR ---
  simulacaoAtual: SimuladorTesourariaDTO | null = null;
  chartInstance: any;
  linhasSimulador: any[] = [];
  saldoAtualTotal: number = 0;

  constructor(
    private tesourariaService: TesourariaService,
    private planeamentoService: PlaneamentoService,
    private analiticaService: AnaliticaService, // Único serviço analítico necessário
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormularios();

    // Subscrição às Contas (Fonte da Verdade)
    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      if (this.contaSelecionada) {
        this.contaSelecionada = contas.find(c => c.id === this.contaSelecionada!.id) || null;
      }
      this.gerarTabelaSimulador();
      this.cd.detectChanges();
    });

    // Subscrição aos Movimentos
    this.tesourariaService.movimentos$.subscribe(movs => {
      this.movimentos = movs;
      this.cd.detectChanges();
    });

    this.carregarDadosIniciais();
  }

  ngAfterViewInit() {
    this.carregarSimulador();
  }

  private carregarDadosIniciais() {
    this.tesourariaService.carregarContasDaAPI();
    this.carregarPendentes();
    this.carregarEntidades();
    this.carregarMestresAnaliticos();
    this.carregarPlanos();
  }

  // =========================================================================
  // --- 🚀 MÓDULO DE PLANEAMENTO E ANALÍTICA ---
  // =========================================================================

  carregarMestresAnaliticos() {
    // 🚀 Usa os métodos reais do teu analitica.service.ts
    this.analiticaService.listarCentros().subscribe((res: any) => {
      this.centrosCusto = res.content || res;
    });

    this.analiticaService.listarSeccoes().subscribe((res: any) => {
      this.todasSeccoes = res.content || res;
    });
  }

  carregarPlanos() {
    this.planeamentoService.listarPlanos().subscribe(planos => {
      this.listaPlanos = planos;
      this.cd.detectChanges();
    });
  }

  abrirModalPlaneamento() {
    this.formPlaneamento.reset({
      tipo: TipoMovimentoPlaneado.SAIDA,
      frequencia: FrequenciaMovimento.MENSAL,
      taxaIva: 23,
      valorBase: 0,
      dataInicio: new Date().toISOString().split('T')[0]
    });
    new bootstrap.Modal(document.getElementById('modalPlaneamento')).show();
  }

  guardarPlaneamento() {
    if (this.formPlaneamento.invalid) {
      this.formPlaneamento.markAllAsTouched();
      return;
    }

    this.planeamentoService.criarPlano(this.formPlaneamento.value).subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Planeamento guardado!' });
        bootstrap.Modal.getInstance(document.getElementById('modalPlaneamento'))?.hide();
        this.carregarSimulador();
        this.carregarPlanos();
      },
      error: (e) => Swal.fire('Erro', 'Falha ao guardar o plano estratégico.', 'error')
    });
  }

  alternarStatusPlano(plano: MovimentoPlaneado) {
    this.planeamentoService.alternarStatus(plano.id!).subscribe(() => {
      this.carregarPlanos();
      this.carregarSimulador();
    });
  }

  // =========================================================================
  // --- 🚀 MOTOR GRÁFICO & SIMULADOR ---
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

    if (this.chartInstance) this.chartInstance.destroy();

    const labels = dados.pontos.map(p => p.label);
    const saldos = dados.pontos.map(p => p.saldoProjetado || 0);

    const risco = saldos.some(s => s < 0);

    this.chartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Saldo Projetado (€)',
          data: saldos,
          borderColor: risco ? '#dc3545' : '#0d6efd',
          backgroundColor: risco ? 'rgba(220, 53, 69, 0.1)' : 'rgba(13, 110, 253, 0.1)',
          borderWidth: 3,
          tension: 0.4,
          fill: true
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: false } }
      }
    });
  }

  gerarTabelaSimulador() {
    this.saldoAtualTotal = this.listaContas.reduce((acc, c) => acc + c.saldo, 0);
    this.linhasSimulador = [{ isAtual: true, descritivo: 'SALDO ATUAL', saldo: this.saldoAtualTotal }];

    const pendentesOrdenados = [...this.listaPendentes].sort((a, b) => new Date(a.data).getTime() - new Date(b.data).getTime());
    let saldoCorrente = this.saldoAtualTotal;
    
    pendentesOrdenados.forEach(doc => {
      let receita = doc.tipo === 'VENDA' ? doc.valorPendente : null;
      let despesa = doc.tipo === 'COMPRA' ? doc.valorPendente : null;
      if (receita) saldoCorrente += receita;
      if (despesa) saldoCorrente -= despesa;

      this.linhasSimulador.push({
        isAtual: false, data: doc.data, descritivo: doc.entidade,
        receita, despesa, saldo: saldoCorrente, documento: doc 
      });
    });
  }

  // =========================================================================
  // --- GESTÃO DE FORMULÁRIOS & REGRAS ---
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

    // 🚀 Form de Planeamento
    this.formPlaneamento = this.fb.group({
      descricao: ['', Validators.required],
      tipo: [TipoMovimentoPlaneado.SAIDA, Validators.required],
      frequencia: [FrequenciaMovimento.MENSAL, Validators.required],
      valorBase: [0, [Validators.required, Validators.min(0.01)]],
      taxaIva: [23, Validators.required],
      dataInicio: [new Date().toISOString().split('T')[0], Validators.required],
      dataFim: [null],
      centroCustoId: [null, Validators.required],
      seccaoHomoId: [null, Validators.required],
      clienteId: [null],
      fornecedorId: [null]
    });

    this.configurarValidacoesDinamicas();
  }

  private configurarValidacoesDinamicas() {
    // Para Movimentos Reais
    this.formMovimento.get('tipo')?.valueChanges.subscribe(tipo => {
      this.aplicarRegrasParceiro(this.formMovimento, tipo);
    });

    // Para Planeamento
    this.formPlaneamento.get('tipo')?.valueChanges.subscribe(tipo => {
      this.aplicarRegrasParceiro(this.formPlaneamento, tipo);
    });
  }

  private aplicarRegrasParceiro(form: FormGroup, tipo: string) {
    const cli = form.get('clienteId');
    const fornc = form.get('fornecedorId');
    if (tipo === 'DEBITO' || tipo === 'SAIDA') {
      fornc?.setValidators(Validators.required);
      cli?.clearValidators();
      cli?.setValue(null);
    } else {
      cli?.setValidators(Validators.required);
      fornc?.clearValidators();
      fornc?.setValue(null);
    }
    cli?.updateValueAndValidity();
    fornc?.updateValueAndValidity();
  }

  // =========================================================================
  // --- ACÇÕES FINANCEIRAS CORE (Contas, Movimentos, etc) ---
  // =========================================================================

  carregarEntidades() {
    this.clienteService.listar().subscribe((res: any) => this.clientes = res.content || res);
    this.fornecedorService.listar().subscribe((res: any) => this.fornecedores = res.content || res);
  }

  carregarPendentes() {
    this.tesourariaService.listarPendentes().subscribe({
      next: (dados: DocumentoPendente[]) => {
        this.listaPendentes = dados;
        this.gerarTabelaSimulador();
        this.cd.detectChanges();
      },
      error: (e: HttpErrorResponse) => console.error('Erro ao carregar pendentes:', e.message)
    });
  }

  abrirModalConfirmacao(doc: DocumentoPendente) {
    this.docParaConfirmar = doc;
    this.formConfirmacao.reset({ 
      dataPagamento: this.getDataAtual(), 
      contaBancariaId: this.listaContas[0]?.id,
      valorAPagar: doc.valorPendente
    });
    new bootstrap.Modal(document.getElementById('modalConfirmacao')).show();
  }

  confirmarTransacao() {
    if (this.formConfirmacao.invalid || !this.docParaConfirmar) return;
    this.tesourariaService.confirmarTransacao({
      ...this.formConfirmacao.value,
      documentoId: this.docParaConfirmar.id,
      tipoDocumento: this.docParaConfirmar.tipo
    }).subscribe(() => {
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Liquidado!', timer: 2000 });
      bootstrap.Modal.getInstance(document.getElementById('modalConfirmacao'))?.hide();
      this.carregarDadosIniciais();
      if (this.abaAtiva === 'simulador') this.carregarSimulador();
    });
  }

  verExtrato(conta: ContaBancaria) {
    this.contaSelecionada = conta;
    this.tesourariaService.obterExtrato(conta.id!);
  }

  novaConta() {
    this.formConta.reset({ saldoInicial: 0, iban: 'PT50' }); 
    new bootstrap.Modal(document.getElementById('modalConta')).show();
  }

  guardarConta() {
    if (this.formConta.invalid) return;
    this.tesourariaService.criarConta(this.formConta.value).subscribe(() => {
      bootstrap.Modal.getInstance(document.getElementById('modalConta'))?.hide();
      this.carregarSimulador();
    });
  }

  novoMovimento() {
    this.formMovimento.reset({ contaId: this.contaSelecionada?.id, tipo: 'DEBITO', valor: 0 });
    this.aplicarRegrasParceiro(this.formMovimento, 'DEBITO');
    new bootstrap.Modal(document.getElementById('modalMovimento')).show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) return;
    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe(() => {
      bootstrap.Modal.getInstance(document.getElementById('modalMovimento'))?.hide();
      if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
      this.carregarSimulador();
    });
  }

  novaTransferencia() {
    this.formTransferencia.reset({ contaOrigemId: this.contaSelecionada?.id, valor: 0 });
    new bootstrap.Modal(document.getElementById('modalTransferencia')).show();
  }

  realizarTransferencia() {
    if (this.formTransferencia.invalid) return;
    this.tesourariaService.realizarTransferencia(this.formTransferencia.value).subscribe(() => {
      bootstrap.Modal.getInstance(document.getElementById('modalTransferencia'))?.hide();
      if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
    });
  }

  anularMovimento(mov: Movimento) {
    Swal.fire({ title: 'Anular?', text: 'Saldo será revertido.', icon: 'warning', showCancelButton: true }).then(r => {
      if (r.isConfirmed) {
        this.tesourariaService.anularMovimento(mov.id!).subscribe(() => {
          if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
          this.carregarPendentes();
          this.carregarSimulador();
        });
      }
    });
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }
}