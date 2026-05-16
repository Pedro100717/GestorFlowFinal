import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router'; 

import { TesourariaService } from '../../services/tesouraria.service';
import { ClienteService } from '../../services/cliente.service'; 
import { FornecedorService } from '../../services/fornecedor.service';
import { PlaneamentoService } from '../../services/planeamento.service';

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
  planoEmEdicaoId: number | null = null; 

  // --- DADOS REAIS ---
  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;
  listaPendentes: DocumentoPendente[] = []; 
  docParaConfirmar: DocumentoPendente | null = null; 

  // --- DADOS DE PLANEAMENTO ---
  listaPlanos: MovimentoPlaneado[] = [];
  clientes: Cliente[] = [];
  fornecedores: Fornecedor[] = [];

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
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit() {
    this.inicializarFormularios();

    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      if (this.contaSelecionada) {
        this.contaSelecionada = contas.find(c => c.id === this.contaSelecionada!.id) || null;
      }
      this.gerarTabelaSimulador();
      this.cd.detectChanges();
    });

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
    this.carregarPlanos();
  }

  // =========================================================================
  // --- 🚀 MÓDULO DE PLANEAMENTO (CASH FLOW PURO) ---
  // =========================================================================

  carregarPlanos() {
    this.planeamentoService.listarPlanos().subscribe(planos => {
      this.listaPlanos = planos;
      this.gerarTabelaSimulador(); 
      this.cd.detectChanges();
    });
  }

  abrirModalPlaneamento(plano?: MovimentoPlaneado) {
    if (plano) {
      this.planoEmEdicaoId = plano.id || null;
      this.formPlaneamento.patchValue({
        descricao: plano.descricao,
        tipo: plano.tipo,
        frequencia: plano.frequencia,
        valorBase: plano.valorBase,
        dataInicio: plano.dataInicio,
        dataFim: plano.dataFim
      });
    } else {
      this.planoEmEdicaoId = null;
      this.formPlaneamento.reset({
        tipo: TipoMovimentoPlaneado.SAIDA,
        frequencia: FrequenciaMovimento.MENSAL,
        valorBase: 0,
        dataInicio: new Date().toISOString().split('T')[0]
      });
    }
    new bootstrap.Modal(document.getElementById('modalPlaneamento')).show();
  }

  guardarPlaneamento() {
    if (this.formPlaneamento.invalid) {
      this.formPlaneamento.markAllAsTouched();
      return;
    }

    const operacao = this.planoEmEdicaoId
      ? this.planeamentoService.atualizarPlano(this.planoEmEdicaoId, this.formPlaneamento.value)
      : this.planeamentoService.criarPlano(this.formPlaneamento.value);

    operacao.subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Planeamento guardado!' });
        bootstrap.Modal.getInstance(document.getElementById('modalPlaneamento'))?.hide();
        this.carregarPlanos();
        this.carregarSimulador();
      },
      error: (e) => Swal.fire('Erro', 'Falha ao guardar o planeamento.', 'error')
    });
  }

  apagarPlano(plano: MovimentoPlaneado) {
    Swal.fire({
      title: 'Apagar Plano?',
      text: `Queres mesmo apagar "${plano.descricao}"? Isto vai recalcular o teu gráfico instantaneamente.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sim, Apagar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#dc3545'
    }).then((result) => {
      if (result.isConfirmed && plano.id) {
        this.planeamentoService.apagarPlano(plano.id).subscribe({
          next: () => {
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Plano apagado!', timer: 2000, showConfirmButton: false });
            this.carregarPlanos();
            this.carregarSimulador();
          },
          error: () => Swal.fire('Erro', 'Não foi possível apagar o plano.', 'error')
        });
      }
    });
  }

  alternarStatusPlano(plano: MovimentoPlaneado) {
    this.planeamentoService.alternarStatus(plano.id!).subscribe(() => {
      this.carregarPlanos();
      this.carregarSimulador();
    });
  }

  gerarFaturaDoPlano(linha: any) {
    // 🚀 Extrai o plano E a data que estavam na linha!
    const plano = linha.planoAssociado ? linha.planoAssociado : linha; 
    const dataProjetada = linha.data ? linha.data : null; 

    if (!plano || !plano.id) return;

    const isEntrada = plano.tipo === 'ENTRADA';
    const tipoFatura = isEntrada ? 'Venda (Receita)' : 'Compra (Despesa)';
    const rota = isEntrada ? '/app/vendas' : '/app/compras'; 

    Swal.fire({
      title: `Efetivar Previsão?`,
      text: `Vamos encaminhar-te para o ecrã de nova ${tipoFatura} para registares "${plano.descricao}".`,
      icon: 'info',
      showCancelButton: true,
      confirmButtonText: 'Avançar para Registo',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.router.navigate([rota], {
          state: {
            planoOrigemId: plano.id,
            descricao: plano.descricao,
            dataProjetada: dataProjetada // 🚀 AGORA SIM! A data viaja na mala!
          }
        });
      }
    });
  }

  podeGerarFatura(plano: MovimentoPlaneado): boolean {
    if (!plano.dataUltimoProcessamento) return true;
    const dataUltimo = new Date(plano.dataUltimoProcessamento);
    const hoje = new Date();
    return dataUltimo.getMonth() !== hoje.getMonth() || dataUltimo.getFullYear() !== hoje.getFullYear();
  }

  // =========================================================================
  // --- 🚀 MOTOR GRÁFICO & SIMULADOR COM PROJEÇÃO DE TEMPO ---
  // =========================================================================
  
  atualizarGraficoSimulador() {
    setTimeout(() => this.carregarSimulador(), 100);
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
          borderWidth: 3, tension: 0.4, fill: true
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: false } }
      }
    });
  }

  gerarTabelaSimulador() {
    this.saldoAtualTotal = this.listaContas.reduce((acc, c) => acc + c.saldo, 0);
    this.linhasSimulador = [{ isAtual: true, descritivo: 'SALDO ATUAL', saldo: this.saldoAtualTotal, dataObj: new Date() }];

    const itensReais = this.listaPendentes.map(doc => ({
      isAtual: false, isProjecao: false,
      dataObj: new Date(doc.data), data: doc.data,
      
      descritivo: doc.entidade,
      // 🚀 AQUI ESTÁ! O TRANSPORTE DA DESCRIÇÃO PARA A PREVISÃO!
      descricao: doc.descricao, 
      
      documento: doc,
      receita: doc.tipo === 'VENDA' || doc.tipo === 'RECEITA' ? doc.valorPendente : null,
      despesa: doc.tipo === 'COMPRA' || doc.tipo === 'DESPESA' ? doc.valorPendente : null
    }));

    const projecoes: any[] = [];
    const fimDoAno = new Date(new Date().getFullYear(), 11, 31);

    this.listaPlanos.filter(p => p.ativo !== false).forEach(plano => {
      let dataCursor = plano.dataUltimoProcessamento ? new Date(plano.dataUltimoProcessamento) : new Date(plano.dataInicio);
      
      if (plano.dataUltimoProcessamento) {
        dataCursor = this.adicionarFrequencia(dataCursor, plano.frequencia as string);
      }

      const dataFimLimite = plano.dataFim ? new Date(plano.dataFim) : fimDoAno;
      const limiteReal = dataFimLimite < fimDoAno ? dataFimLimite : fimDoAno;

      while (dataCursor <= limiteReal) {
        projecoes.push({
          isAtual: false, isProjecao: true, planoAssociado: plano,
          dataObj: new Date(dataCursor), data: dataCursor.toISOString(),
          descritivo: plano.descricao + ' (Previsão)',
          receita: plano.tipo === 'ENTRADA' ? plano.valorBase : null, 
          despesa: plano.tipo === 'SAIDA' ? plano.valorBase : null
        });

        if (plano.frequencia === 'PONTUAL') break;
        dataCursor = this.adicionarFrequencia(dataCursor, plano.frequencia as string);
      }
    });

    const tudoMisturado = [...itensReais, ...projecoes].sort((a, b) => a.dataObj.getTime() - b.dataObj.getTime());

    let saldoCorrente = this.saldoAtualTotal;
    tudoMisturado.forEach(linha => {
      if (linha.receita) saldoCorrente += linha.receita;
      if (linha.despesa) saldoCorrente -= linha.despesa;
      linha.saldo = saldoCorrente;
      this.linhasSimulador.push(linha);
    });
  }

  private adicionarFrequencia(data: Date, frequencia: string): Date {
    const novaData = new Date(data);
    switch(frequencia) {
      case 'SEMANAL': novaData.setDate(novaData.getDate() + 7); break;
      case 'MENSAL': novaData.setMonth(novaData.getMonth() + 1); break;
      case 'TRIMESTRAL': novaData.setMonth(novaData.getMonth() + 3); break;
      case 'SEMESTRAL': novaData.setMonth(novaData.getMonth() + 6); break;
      case 'ANUAL': novaData.setFullYear(novaData.getFullYear() + 1); break;
    }
    return novaData;
  }

  editarDataPagamento(linha: any) {
    const dataAtual = linha.data.split('T')[0];

    Swal.fire({
      title: 'Alterar Previsão de Pagamento',
      html: `
        <p class="small text-muted mb-3">A data de emissão original será mantida para a contabilidade.</p>
        <input type="date" id="nova-data" class="form-control swal2-input mx-auto" style="max-width: 250px;" value="${dataAtual}">
      `,
      showCancelButton: true,
      confirmButtonText: 'Atualizar Gráfico',
      cancelButtonText: 'Cancelar',
      preConfirm: () => {
        const novaData = (document.getElementById('nova-data') as HTMLInputElement).value;
        if (!novaData) return Swal.showValidationMessage('Por favor, escolha uma data.');
        return novaData;
      }
    }).then((result) => {
      if (result.isConfirmed) {
        this.tesourariaService.alterarDataPrevista(linha.documento.id, linha.documento.tipo, result.value).subscribe({
          next: () => {
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Data atualizada!', timer: 2000, showConfirmButton: false });
            this.carregarPendentes();
            this.carregarSimulador();
          },
          error: () => Swal.fire('Erro', 'Não foi possível alterar a previsão.', 'error')
        });
      }
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

    this.formPlaneamento = this.fb.group({
      descricao: ['', Validators.required],
      tipo: [TipoMovimentoPlaneado.SAIDA, Validators.required],
      frequencia: [FrequenciaMovimento.MENSAL, Validators.required],
      valorBase: [0, [Validators.required, Validators.min(0.01)]],
      dataInicio: [new Date().toISOString().split('T')[0], Validators.required],
      dataFim: [null]
    });

    this.configurarValidacoesDinamicas();
  }

  private configurarValidacoesDinamicas() {
    this.formMovimento.get('tipo')?.valueChanges.subscribe(tipo => this.aplicarRegrasParceiro(this.formMovimento, tipo));
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
  // --- ACÇÕES FINANCEIRAS CORE ---
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