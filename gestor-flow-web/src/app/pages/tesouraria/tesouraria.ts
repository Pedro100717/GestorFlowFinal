import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router'; 

// 🚀 IMPORTAÇÃO DO MOTOR DE ANIMAÇÕES NATIVO DO ANGULAR
import { trigger, state, style, transition, animate } from '@angular/animations';

import { TesourariaService } from '../../services/tesouraria.service';
import { ClienteService } from '../../services/cliente.service'; 
import { FornecedorService } from '../../services/fornecedor.service';
import { PlaneamentoService } from '../../services/planeamento.service';
import { LogService } from '../../core/services/log.service'; // 🚀 INJEÇÃO DO NOSSO INSPETOR

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

export interface LinhaSimulador {
  isAtual: boolean;
  isProjecao: boolean;
  dataObj: Date;
  data: string;
  descritivo: string;
  descricao?: string;
  receita?: number | null;
  despesa?: number | null;
  saldo?: number;
  planoAssociado?: MovimentoPlaneado; 
  documento?: DocumentoPendente;      
}

@Component({
  selector: 'app-tesouraria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tesouraria.html',
  styleUrls: ['./tesouraria.scss'],
  animations: [
    trigger('expandirTabela', [
      state('normal', style({})),
      state('expandido', style({
        position: 'fixed',
        top: '80px', left: '260px', right: '20px', bottom: '20px',
        zIndex: 1040,
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.4)',
        border: '2px solid #0d6efd',
        borderRadius: '12px',
        backgroundColor: 'white',
        opacity: 1,
        transform: 'scale(1) translateY(0)'
      })),
      
      transition('normal => expandido', [
        style({ 
          position: 'fixed', top: '80px', left: '260px', right: '20px', bottom: '20px',
          zIndex: 1040, backgroundColor: 'white',
          opacity: 0, 
          transform: 'scale(0.95) translateY(25px)' 
        }),
        animate('400ms cubic-bezier(0.25, 1, 0.5, 1)')
      ]),

      transition('expandido => normal', [
        animate('150ms ease-in-out', style({ 
          opacity: 0, 
          transform: 'scale(0.96) translateY(10px)' 
        }))
      ])
    ])
  ]
})
export class TesourariaComponent implements OnInit, AfterViewInit {

  abaAtiva: 'contas' | 'pendentes' | 'simulador' = 'contas'; 
  
  isTableExpanded: boolean = false;
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
  formFiltros!: FormGroup;

  // --- SIMULADOR ---
  simulacaoAtual: SimuladorTesourariaDTO | null = null;
  chartInstance: Chart | undefined;
  linhasSimulador: LinhaSimulador[] = []; 
  linhasSimuladorFiltradas: LinhaSimulador[] = []; 
  
  saldoAtualTotal: number = 0;

  constructor(
    private tesourariaService: TesourariaService,
    private planeamentoService: PlaneamentoService,
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private router: Router,
    private logService: LogService // 🚀 SERVIÇO DECLARADO NO CONSTRUTOR
  ) {}

  toggleExpand() {
    this.isTableExpanded = !this.isTableExpanded;
    this.cd.detectChanges();
  }

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

  private parseDataSegura(dataStr: string): Date {
    if (!dataStr) return new Date();
    const cleanStr = dataStr.split('T')[0];
    const [y, m, d] = cleanStr.split('-');
    return new Date(+y, +m - 1, +d, 0, 0, 0, 0); 
  }

  // =========================================================================
  // --- MÓDULO DE PLANEAMENTO ---
  // =========================================================================

  carregarPlanos() {
    this.planeamentoService.listarPlanos().subscribe({
      next: (planos) => {
        this.listaPlanos = planos;
        this.gerarTabelaSimulador(); 
        this.cd.detectChanges();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao carregar planos de planeamento', e); // 🚀 CAIXA NEGRA
      }
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
        dataInicio: new Date().toISOString().split('T')[0],
        dataFim: null
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
        this.logService.info('Planeamento guardado com sucesso.'); // 🚀 RASTREABILIDADE
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Planeamento guardado!' });
        bootstrap.Modal.getInstance(document.getElementById('modalPlaneamento'))?.hide();
        this.carregarPlanos();
        this.carregarSimulador();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Falha ao guardar o planeamento', e); // 🚀 CAIXA NEGRA
        Swal.fire('Erro', 'Falha ao guardar o planeamento.', 'error');
      }
    });
  }

  apagarPlano(plano: MovimentoPlaneado) {
    Swal.fire({
      title: 'Apagar Plano Base?',
      text: `Queres mesmo apagar a base "${plano.descricao}"?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sim, Apagar',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#dc3545'
    }).then((result) => {
      if (result.isConfirmed && plano.id) {
        this.planeamentoService.apagarPlano(plano.id).subscribe({
          next: () => {
            this.logService.info(`Plano apagado com sucesso: ID ${plano.id}`); // 🚀 RASTREABILIDADE
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Plano apagado!', timer: 2000, showConfirmButton: false });
            this.carregarPlanos();
            this.carregarSimulador();
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Falha ao apagar o plano ID ${plano.id}`, e); // 🚀 CAIXA NEGRA
            Swal.fire('Erro', 'Não foi possível apagar o plano.', 'error');
          }
        });
      }
    });
  }

  alternarStatusPlano(plano: MovimentoPlaneado) {
    this.planeamentoService.alternarStatus(plano.id!).subscribe({
      next: () => {
        this.logService.debug(`Status alternado para o plano ID ${plano.id}`); // 🚀 RASTREABILIDADE
        this.carregarPlanos();
        this.carregarSimulador();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error(`Erro ao alternar status do plano ID ${plano.id}`, e); // 🚀 CAIXA NEGRA
      }
    });
  }

  apagarLinhaProjecao(linha: LinhaSimulador) { 
    const planoOriginal = mergeLinhaPlano(linha);
    const dataProjetada = linha.data.split('T')[0];

    if (planoOriginal.frequencia === 'PONTUAL') {
      this.apagarPlano(planoOriginal);
      return;
    }

    Swal.fire({
      title: 'Apagar Previsão',
      text: `Esta previsão faz parte do plano "${planoOriginal.descricao}". Queres apagar apenas o mês de ${this.formatarMesAno(dataProjetada)} ou cancelar o plano para sempre?`,
      icon: 'warning',
      showDenyButton: true,
      showCancelButton: true,
      confirmButtonText: 'Só este Mês',
      denyButtonText: 'Para Sempre',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#0d6efd',
      denyButtonColor: '#dc3545'
    }).then((result) => {
      if (result.isConfirmed) {
        this.planeamentoService.ignorarDataPlano(planoOriginal.id!, dataProjetada).subscribe({
          next: () => {
            this.logService.info(`Data ${dataProjetada} ignorada para o plano ${planoOriginal.id}`); // 🚀 RASTREABILIDADE
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Mês ignorado!', timer: 2000, showConfirmButton: false });
            this.carregarPlanos();
            this.carregarSimulador();
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Erro ao ignorar data do plano ${planoOriginal.id}`, e); // 🚀 CAIXA NEGRA
            Swal.fire('Erro', 'Não foi possível ignorar a data.', 'error');
          }
        });
      } else if (result.isDenied) {
        this.apagarPlano(planoOriginal);
      }
    });
  }

  editarLinhaProjecao(linha: LinhaSimulador) { 
    const planoOriginal = mergeLinhaPlano(linha);
    const dataProjetadaOriginal = linha.data.split('T')[0];
    const valorAtual = planoOriginal.valorBase;
    const descricaoAtual = planoOriginal.descricao;

    Swal.fire({
      title: 'Editar Previsão',
      html: `
        <div class="text-start px-3 mt-2">
          <label class="form-label small fw-bold mb-1 text-muted">Descrição da Previsão</label>
          <input type="text" id="nova-desc-plano" class="form-control mb-3 border-secondary" value="${descricaoAtual}">
          <div class="row">
              <div class="col-6">
                  <label class="form-label small fw-bold mb-1 text-muted">Data Prevista</label>
                  <input type="date" id="nova-data-plano" class="form-control border-secondary" value="${dataProjetadaOriginal}">
              </div>
              <div class="col-6">
                  <label class="form-label small fw-bold mb-1 text-muted">Valor Projetado (€)</label>
                  <input type="number" id="novo-valor-plano" class="form-control border-secondary" value="${valorAtual}" step="0.01">
              </div>
          </div>
        </div>
      `,
      showCancelButton: true,
      confirmButtonText: 'Avançar',
      cancelButtonText: 'Cancelar',
      width: '32em',
      preConfirm: () => {
        const desc = (document.getElementById('nova-desc-plano') as HTMLInputElement).value;
        const data = (document.getElementById('nova-data-plano') as HTMLInputElement).value;
        const val = (document.getElementById('novo-valor-plano') as HTMLInputElement).value;

        if (!desc) return Swal.showValidationMessage('A descrição não pode estar vazia.');
        if (!data) return Swal.showValidationMessage('A data é obrigatória.');
        if (!val || Number(val) <= 0) return Swal.showValidationMessage('Insira um valor maior que 0.');

        return { descricao: desc, data: data, valor: Number(val) };
      }
    }).then((resultValor) => {
      if (resultValor.isConfirmed) {
        const dadosEditados = resultValor.value;

        if (planoOriginal.frequencia === 'PONTUAL') {
           const dto = { 
               ...planoOriginal, 
               descricao: dadosEditados.descricao, 
               dataInicio: dadosEditados.data, 
               valorBase: dadosEditados.valor 
           };
           this.planeamentoService.atualizarPlano(planoOriginal.id!, dto).subscribe({
             next: () => {
               this.carregarPlanos();
               this.carregarSimulador();
             },
             error: (e: HttpErrorResponse) => this.logService.error('Erro ao atualizar plano pontual', e)
           });
           return;
        }

        Swal.fire({
          title: 'Aplicar a alteração?',
          text: `Queres que esta alteração seja aplicada APENAS nesta data (${this.formatarMesAno(dataProjetadaOriginal)}), ou queres alterar a regra base para todas as datas futuras?`,
          icon: 'question',
          showDenyButton: true,
          showCancelButton: true,
          confirmButtonText: 'Só nesta Data',
          denyButtonText: 'Em Todas',
          cancelButtonText: 'Cancelar',
          confirmButtonColor: '#0d6efd',
          denyButtonColor: '#198754'
        }).then((resultAcao) => {
          if (resultAcao.isConfirmed) {
            const dtoExcecao = { 
                ...planoOriginal, 
                descricao: dadosEditados.descricao, 
                valorBase: dadosEditados.valor, 
                dataInicio: dadosEditados.data 
            };
            this.planeamentoService.criarExcecaoPlano(planoOriginal.id!, dataProjetadaOriginal, dtoExcecao as MovimentoPlaneado).subscribe({
              next: () => {
                this.logService.info(`Exceção criada para o plano ID ${planoOriginal.id} na data ${dataProjetadaOriginal}`);
                Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Exceção criada com sucesso!', timer: 2000, showConfirmButton: false });
                this.carregarPlanos();
                this.carregarSimulador();
              },
              error: (e: HttpErrorResponse) => {
                this.logService.error('Erro ao criar exceção de plano', e);
                Swal.fire('Erro', 'Não foi possível criar a exceção.', 'error');
              }
            });
          } else if (resultAcao.isDenied) {
            const dtoAtualizado = { 
                ...planoOriginal, 
                descricao: dadosEditados.descricao, 
                valorBase: dadosEditados.valor, 
                dataInicio: dadosEditados.data 
            };
            this.planeamentoService.atualizarPlano(planoOriginal.id!, dtoAtualizado).subscribe({
              next: () => {
                this.logService.info(`Plano base atualizado: ID ${planoOriginal.id}`);
                Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Plano base updated!', timer: 2000, showConfirmButton: false });
                this.carregarPlanos();
                this.carregarSimulador();
              },
              error: (e: HttpErrorResponse) => {
                this.logService.error('Erro ao atualizar plano base', e);
                Swal.fire('Erro', 'Não foi possível atualizar o plano.', 'error');
              }
            });
          }
        });
      }
    });
  }

  formatarMesAno(dataIso: string): string {
    const data = this.parseDataSegura(dataIso);
    return data.toLocaleDateString('pt-PT', { month: 'long', year: 'numeric' });
  }

  gerarFaturaDoPlano(linha: LinhaSimulador) { 
    const plano = mergeLinhaPlano(linha);
    const dataProjetada = inlineDataString(linha); 

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
        this.logService.debug(`A redirecionar planeamento ID ${plano.id} para o ecrã de ${tipoFatura}`);
        this.router.navigate([rota], {
          state: {
            planoOrigemId: plano.id,
            descricao: plano.descricao,
            dataProjetada: dataProjetada 
          }
        });
      }
    });
  }

  // =========================================================================
  // --- MOTOR GRÁFICO & SIMULADOR ---
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
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao carregar simulador de tesouraria', e); // 🚀 CAIXA NEGRA
      }
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
    this.linhasSimulador = [{ 
      isAtual: true, 
      isProjecao: false,
      descritivo: 'SALDO ATUAL', 
      saldo: this.saldoAtualTotal, 
      dataObj: new Date(),
      data: new Date().toISOString()
    }];

    const itensReais: LinhaSimulador[] = this.listaPendentes.map(doc => ({
      isAtual: false, isProjecao: false,
      dataObj: this.parseDataSegura(doc.data), data: doc.data,
      descritivo: doc.entidade,
      descricao: doc.descricao, 
      documento: doc,
      receita: doc.tipo === 'VENDA' || doc.tipo === 'RECEITA' ? doc.valorPendente : null,
      despesa: doc.tipo === 'COMPRA' || doc.tipo === 'DESPESA' ? doc.valorPendente : null
    }));

    const projecoes: LinhaSimulador[] = []; 
    
    const hoje = new Date();
    hoje.setHours(0,0,0,0);
    
    let limiteProjecao = new Date(hoje.getFullYear() + 5, hoje.getMonth(), hoje.getDate());

    this.listaPlanos.filter(p => p.ativo !== false).forEach(p => {
      const dataIni = this.parseDataSegura(p.dataInicio);
      if (dataIni > limiteProjecao) limiteProjecao = new Date(dataIni.getTime());

      if (p.dataFim) {
        const dataF = this.parseDataSegura(p.dataFim);
        if (dataF > limiteProjecao) limiteProjecao = new Date(dataF.getTime());
      }
    });

    const limiteMaximoSeguranca = new Date(hoje.getFullYear() + 10, hoje.getMonth(), hoje.getDate());
    if (limiteProjecao > limiteMaximoSeguranca) {
      limiteProjecao = limiteMaximoSeguranca;
    }

    this.listaPlanos.filter(p => p.ativo !== false).forEach(plano => {
      const diaOriginal = this.parseDataSegura(plano.dataInicio).getDate(); 
      let dataCursor = this.parseDataSegura(plano.dataInicio);
      const dataFimLimite = plano.dataFim ? this.parseDataSegura(plano.dataFim) : limiteProjecao;

      const limiteReal = new Date(dataFimLimite.getTime() < limiteProjecao.getTime() ? dataFimLimite.getTime() : limiteProjecao.getTime());

      let dataUltimoProc = null;
      if (plano.dataUltimoProcessamento) {
          dataUltimoProc = this.parseDataSegura(plano.dataUltimoProcessamento as unknown as string);
      }

      while (dataCursor.getTime() <= limiteReal.getTime()) {
        const ano = dataCursor.getFullYear();
        const mes = String(dataCursor.getMonth() + 1).padStart(2, '0');
        const dia = String(dataCursor.getDate()).padStart(2, '0');
        const cursorDataString = `${ano}-${mes}-${dia}`; 
        
        let jaProcessadoNaRegraAntiga = false;
        if (dataUltimoProc) {
           const anoMesCursorVal = ano * 12 + dataCursor.getMonth();
           const anoMesUltimoProcessamento = dataUltimoProc.getFullYear() * 12 + dataUltimoProc.getMonth();
           jaProcessadoNaRegraAntiga = anoMesCursorVal <= anoMesUltimoProcessamento;
        }

        const estaIgnoradoNaMaquinaDoTempo = plano.datasIgnoradas?.includes(cursorDataString);

        if (!jaProcessadoNaRegraAntiga && !estaIgnoradoNaMaquinaDoTempo) {
          projecoes.push({
            isAtual: false, isProjecao: true, planoAssociado: plano,
            dataObj: new Date(dataCursor.getTime()), 
            data: cursorDataString, 
            descritivo: plano.descricao + ' (Previsão)',
            receita: plano.tipo === 'ENTRADA' ? plano.valorBase : null, 
            despesa: plano.tipo === 'SAIDA' ? plano.valorBase : null
          });
        }

        if (plano.frequencia === 'PONTUAL') break;
        dataCursor = this.adicionarFrequencia(dataCursor, plano.frequencia as string, diaOriginal); 
      }
    });

    const tudoMisturado = [...itensReais, ...projecoes].sort((a, b) => a.dataObj.getTime() - b.dataObj.getTime());

    let saldoCorrente = this.saldoAtualTotal;
    tudoMisturado.forEach(linha => {
      if (linha.receita) saldoCorrente += inlineVal(linha.receita);
      if (linha.despesa) saldoCorrente -= inlineVal(linha.despesa);
      linha.saldo = saldoCorrente;
      this.linhasSimulador.push(linha);
    });

    this.aplicarFiltrosTabela(); 
  }

  private adicionarFrequencia(dataAtual: Date, frequencia: string, diaOriginal: number): Date {
    const novaData = new Date(dataAtual.getTime());

    if (frequencia === 'PONTUAL') return novaData;
    if (frequencia === 'SEMANAL') {
      novaData.setDate(novaData.getDate() + 7);
      return novaData;
    }

    let mesesASomar = 0;
    switch(frequencia) {
      case 'MENSAL': mesesASomar = 1; break;
      case 'TRIMESTRAL': mesesASomar = 3; break;
      case 'SEMESTRAL': mesesASomar = 6; break;
      case 'ANUAL': mesesASomar = 12; break;
    }

    const mesAlvo = novaData.getMonth() + mesesASomar;
    novaData.setMonth(mesAlvo, 1); 

    const ultimoDiaDoMes = new Date(novaData.getFullYear(), novaData.getMonth() + 1, 0).getDate();
    const diaCorreto = Math.min(diaOriginal, ultimoDiaDoMes);

    novaData.setDate(diaCorreto);
    return novaData;
  }

  editarDataPagamento(linha: LinhaSimulador) {
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
        this.tesourariaService.alterarDataPrevista(linha.documento!.id, linha.documento!.tipo, result.value).subscribe({
          next: () => {
            this.logService.info(`Data prevista alterada para o documento ID ${linha.documento!.id}`);
            Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Data updated!', timer: 2000, showConfirmButton: false });
            this.carregarPendentes();
            this.carregarSimulador();
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error('Erro ao alterar data prevista do documento', e);
            Swal.fire('Erro', 'Não foi possível alterar a previsão.', 'error');
          }
        });
      }
    });
  }

  // =========================================================================
  // --- GESTÃO DE FORMULÁRIOS E FILTROS ---
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
      dataFim: [null, Validators.required] 
    });

    this.formFiltros = this.fb.group({
      fluxo: ['TUDO'],
      natureza: ['TUDO'],
      periodo: ['TUDO']
    });

    this.configurarValidacoesDinamicas();
  }

  private configurarValidacoesDinamicas() {
    this.formMovimento.get('tipo')?.valueChanges.subscribe(tipo => this.aplicarRegrasParceiro(this.formMovimento, tipo));

    this.formPlaneamento.get('frequencia')?.valueChanges.subscribe(freq => {
      const ctrlDataFim = this.formPlaneamento.get('dataFim');
      if (freq === 'PONTUAL') {
        ctrlDataFim?.clearValidators();
        ctrlDataFim?.setValue(null);
      } else {
        ctrlDataFim?.setValidators(Validators.required);
      }
      ctrlDataFim?.updateValueAndValidity();
    });

    this.formFiltros.valueChanges.subscribe(() => this.aplicarFiltrosTabela());
  }

  aplicarFiltrosTabela() {
    if (!this.formFiltros) return;
    
    const filtros = this.formFiltros.value;
    const hoje = new Date();
    hoje.setHours(0,0,0,0);

    this.linhasSimuladorFiltradas = this.linhasSimulador.filter(linha => {
      if (linha.isAtual) return true;

      if (filtros.fluxo === 'ENTRADAS' && !linha.receita) return false;
      if (filtros.fluxo === 'SAIDAS' && !linha.despesa) return false;

      if (filtros.natureza === 'REAIS' && linha.isProjecao) return false;
      if (filtros.natureza === 'PLANOS' && !linha.isProjecao) return false;

      if (filtros.periodo !== 'TUDO') {
        const dataLinha = new Date(linha.dataObj);
        
        if (filtros.periodo === '6_MESES') {
          const limite = new Date(hoje.getFullYear(), hoje.getMonth() + 6, hoje.getDate());
          if (dataLinha > limite) return false;
        }
        else if (filtros.periodo === '1_ANO') {
          const limite = new Date(hoje.getFullYear() + 1, hoje.getMonth(), hoje.getDate());
          if (dataLinha > limite) return false;
        }
        else if (filtros.periodo === '2_ANOS') {
          const limite = new Date(hoje.getFullYear() + 2, hoje.getMonth(), hoje.getDate());
          if (dataLinha > limite) return false;
        }
        else if (filtros.periodo === '3_ANOS') {
          const limite = new Date(hoje.getFullYear() + 3, hoje.getMonth(), hoje.getDate());
          if (dataLinha > limite) return false;
        }
        else if (filtros.periodo === '5_ANOS') {
          const limite = new Date(hoje.getFullYear() + 5, hoje.getMonth(), hoje.getDate());
          if (dataLinha > limite) return false;
        }
      }

      return true; 
    });
  }

  exportarEvolucaoPdf() {
    const filtros = this.formFiltros.value;

    Swal.fire({
      title: 'A gerar documento...',
      text: 'A compilar a linha do tempo e a calcular saldos.',
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });

    this.tesourariaService.extrairEvolucaoPdf(filtros.fluxo, filtros.natureza, filtros.periodo).subscribe({
      next: (blob: Blob) => {
        Swal.close();
        this.logService.info('PDF de evolução de tesouraria exportado com sucesso.');
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (err: HttpErrorResponse) => {
        this.logService.error('Erro ao gerar PDF de evolução de tesouraria', err); // 🚀 CAIXA NEGRA
        Swal.fire('Erro', 'Não foi possível extrair a evolução em PDF.', 'error');
      }
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

  carregarEntidades() {
    this.clienteService.listar().subscribe({
      next: (res: unknown) => {
        const pageRes = res as { content?: Cliente[] };
        this.clientes = pageRes.content ? pageRes.content : (res as Cliente[]);
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar lista de clientes na tesouraria', e)
    });

    this.fornecedorService.listar().subscribe({
      next: (res: unknown) => {
        const pageRes = res as { content?: Fornecedor[] };
        this.fornecedores = pageRes.content ? pageRes.content : (res as Fornecedor[]);
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar lista de fornecedores na tesouraria', e)
    });
  }

  carregarPendentes() {
    this.tesourariaService.listarPendentes().subscribe({
      next: (dados: DocumentoPendente[]) => {
        this.listaPendentes = dados;
        this.gerarTabelaSimulador();
        this.cd.detectChanges();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao carregar documentos pendentes de tesouraria', e); // 🚀 CAIXA NEGRA
      }
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
    }).subscribe({
      next: () => {
        this.logService.info(`Documento pendente liquidado: ID ${this.docParaConfirmar?.id}`);
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Liquidado!', timer: 2000 });
        bootstrap.Modal.getInstance(document.getElementById('modalConfirmacao'))?.hide();
        this.carregarDadosIniciais();
        if (this.abaAtiva === 'simulador') this.carregarSimulador();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao confirmar transação de pagamento', e);
        Swal.fire('Erro', 'Não foi possível efetuar o pagamento.', 'error');
      }
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
    this.tesourariaService.criarConta(this.formConta.value).subscribe({
      next: () => {
        this.logService.info('Nova conta bancária criada.');
        bootstrap.Modal.getInstance(document.getElementById('modalConta'))?.hide();
        this.carregarSimulador();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao criar conta bancária', e);
        Swal.fire('Erro', 'Não foi possível criar a conta.', 'error');
      }
    });
  }

  novoMovimento() {
    this.formMovimento.reset({ contaId: this.contaSelecionada?.id, tipo: 'DEBITO', valor: 0 });
    this.aplicarRegrasParceiro(this.formMovimento, 'DEBITO');
    new bootstrap.Modal(document.getElementById('modalMovimento')).show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) return;
    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe({
      next: () => {
        this.logService.info('Movimento manual de tesouraria registado.');
        bootstrap.Modal.getInstance(document.getElementById('modalMovimento'))?.hide();
        if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
        this.carregarSimulador();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao registar movimento manual', e);
        Swal.fire('Erro', 'Não foi possível registar o movimento.', 'error');
      }
    });
  }

  novaTransferencia() {
    this.formTransferencia.reset({ contaOrigemId: this.contaSelecionada?.id, valor: 0 });
    new bootstrap.Modal(document.getElementById('modalTransferencia')).show();
  }

  realizarTransferencia() {
    if (this.formTransferencia.invalid) return;
    this.tesourariaService.realizarTransferencia(this.formTransferencia.value).subscribe({
      next: () => {
        this.logService.info('Transferência entre contas bancárias efetuada.');
        bootstrap.Modal.getInstance(document.getElementById('modalTransferencia'))?.hide();
        if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Erro ao realizar transferência bancária', e);
        Swal.fire('Erro', 'Não foi possível realizar a transferência.', 'error');
      }
    });
  }

  anularMovimento(mov: Movimento) {
    Swal.fire({ title: 'Anular?', text: 'Saldo será revertido.', icon: 'warning', showCancelButton: true }).then(r => {
      if (r.isConfirmed) {
        this.tesourariaService.anularMovimento(mov.id!).subscribe({
          next: () => {
            this.logService.warn(`Movimento de tesouraria anulado: ID ${mov.id}`);
            if (this.contaSelecionada) this.verExtrato(this.contaSelecionada);
            this.carregarPendentes();
            this.carregarSimulador();
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Erro ao anular movimento ID ${mov.id}`, e);
            Swal.fire('Erro', 'Não foi possível anular o movimento.', 'error');
          }
        });
      }
    });
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 10);
  }
}

function mergeLinhaPlano(l: LinhaSimulador): MovimentoPlaneado {
  return l.planoAssociado!;
}

function inlineVal(v: number | null | undefined): number {
  return v || 0;
}

function inlineDataString(l: LinhaSimulador): string {
  return l.data.split('T')[0];
}