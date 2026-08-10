import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO DOS ERROS
import { StockService } from '../../services/stock.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { FornecedorService } from '../../services/fornecedor.service';
import { LogService } from '../../core/services/log.service'; // 🚀 INJEÇÃO DO INSPETOR

import { MovimentoStock } from '../../core/models/stock.model';
import { Artigo } from '../../core/models/artigo.model';
import { Cliente } from '../../core/models/cliente.model';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { forkJoin } from 'rxjs'; 

import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-acertos-stock',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './acertos-stock.html',
  styleUrls: ['./acertos-stock.scss']
})
export class AcertosStockComponent implements OnInit {

  abaAtiva: 'inventario' | 'historico' = 'inventario';

  listaHistorico: MovimentoStock[] = [];
  listaMercadorias: Artigo[] = []; 
  
  // 🛡️ VARIÁVEIS DO MODAL NATIVO
  mostrarModalHistorico: boolean = false;
  historicoArtigoSelecionado: MovimentoStock[] = [];
  artigoSelecionadoParaHistorico: string = '';
  
  listaClientes: Cliente[] = []; 
  listaFornecedores: Fornecedor[] = []; 
  
  formAcerto!: FormGroup;

  constructor(
    private stockService: StockService,
    private artigoService: ArtigoService,
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 SERVIÇO DECLARADO NO CONSTRUTOR
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDadosIniciais();

    this.stockService.historico$.subscribe((lista: MovimentoStock[]) => {
      this.listaHistorico = lista;
      this.cd.detectChanges();
    });

    this.artigoService.artigos$.subscribe((lista) => {
      this.listaMercadorias = lista.filter(a => a.movimentaStock === true || a.tipo === 'MERCADORIA');
      this.cd.detectChanges();
    });

    // OUVINTE DINÂMICO PARA AS VALIDAÇÕES OBRIGATÓRIAS
    this.formAcerto.get('tipo')?.valueChanges.subscribe(tipoSelecionado => {
        const controlCliente = this.formAcerto.get('clienteId');
        const controlFornecedor = this.formAcerto.get('fornecedorId');

        if (tipoSelecionado === 'ENTRADA') {
            controlCliente?.setValidators([Validators.required]);
            controlFornecedor?.clearValidators();
            controlFornecedor?.setValue(null);
        } else if (tipoSelecionado === 'SAIDA') {
            controlFornecedor?.setValidators([Validators.required]);
            controlCliente?.clearValidators();
            controlCliente?.setValue(null);
        }

        controlCliente?.updateValueAndValidity();
        controlFornecedor?.updateValueAndValidity();
    });
  }

  inicializarFormulario() {
    this.formAcerto = this.fb.group({
      mercadoriaId: [null, [Validators.required]],
      tipo: ['ENTRADA', [Validators.required]],
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      motivo: ['', [Validators.required]],
      clienteId: [null, [Validators.required]],
      fornecedorId: [null]
    });
  }

  get f() { return this.formAcerto.controls; }

  carregarDadosIniciais() {
    this.stockService.carregarHistoricoDaAPI();
    this.artigoService.carregarArtigosDaAPI();
    
    forkJoin({
        clientes: this.clienteService.listar(),
        fornecedores: this.fornecedorService.listar()
    }).subscribe({
      next: (res) => {
        // 🚀 ADEUS ANY: Tratamento nativo com o fallback content
        this.listaClientes = res.clientes.content || res.clientes;
        this.listaFornecedores = res.fornecedores.content || res.fornecedores;
        this.logService.debug('Dados de Clientes e Fornecedores carregados para Acertos de Stock.');
        this.cd.detectChanges();
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar dados auxiliares do stock', e)
    });
  }

  abrirModalNovo(mercadoriaIdPadrao: number | null = null) {
    this.formAcerto.reset({ 
        mercadoriaId: mercadoriaIdPadrao,
        tipo: 'ENTRADA', 
        quantidade: 1, 
        clienteId: null, 
        fornecedorId: null 
    });
    const modal = new bootstrap.Modal(document.getElementById('modalAcerto'));
    modal.show();
  }

  abrirHistoricoArtigo(artigo: Artigo) {
    this.artigoSelecionadoParaHistorico = artigo.nome;
    this.historicoArtigoSelecionado = []; 
    
    this.mostrarModalHistorico = true; 
    
    this.stockService.obterHistoricoDoArtigo(artigo.id!).subscribe({
      // 🚀 ADEUS ANY E CONSOLE.LOGS AMADORES
      next: (res) => {
        this.historicoArtigoSelecionado = res.content || res || []; 
        this.logService.debug(`Histórico carregado para o artigo ${artigo.id}.`); // 🚀 RASTREABILIDADE
        this.cd.detectChanges(); 
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error(`Erro ao buscar histórico do artigo ${artigo.id}`, err); // 🚀 CAIXA NEGRA
        Swal.fire('Erro', 'Não foi possível carregar o histórico deste artigo.', 'error');
        this.mostrarModalHistorico = false; 
      }
    });
  }

  fecharModalHistorico() {
    this.mostrarModalHistorico = false;
  }

  guardarAcerto() {
    if (this.formAcerto.invalid) {
      this.formAcerto.markAllAsTouched();
      return;
    }

    const formValues = this.formAcerto.value;
    const artigo = this.listaMercadorias.find(a => a.id === formValues.mercadoriaId);

    if (formValues.tipo === 'SAIDA' && artigo && (artigo.stockAtual || 0) < formValues.quantidade) {
        Swal.fire({
          title: 'Atenção ao Stock!',
          text: `Tens apenas ${artigo.stockAtual} unidades. Queres forçar a saída de ${formValues.quantidade} unidades? O stock ficará negativo.`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonColor: '#dc3545',
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sim, registar saída!',
          cancelButtonText: 'Cancelar'
        }).then((result) => {
          if (result.isConfirmed) {
            this.logService.warn(`Acerto de stock vai forçar saldo negativo no artigo ${artigo.id}.`); // 🚀 RASTREABILIDADE DE RISCO
            this.executarRegisto(formValues);
          }
        });
    } else {
        this.executarRegisto(formValues);
    }
  }

  private executarRegisto(formValues: Record<string, any>) { // 🚀 RECORD EM VEZ DE ANY SOLTO
    this.stockService.registarAcerto(formValues).subscribe({
      next: (novoAcerto) => {
        this.logService.info(`Acerto de stock registado para o artigo ID ${formValues['mercadoriaId']}. Tipo: ${formValues['tipo']}`); // 🚀 CAIXA NEGRA
        this.artigoService.atualizarStockNaMemoria(formValues['mercadoriaId']!, novoAcerto.stockAposMovimento!);
        
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Acerto registado com sucesso!' });
        
        bootstrap.Modal.getInstance(document.getElementById('modalAcerto'))?.hide();
      },
      error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error('Falha ao registar o acerto de stock', e); // 🚀 CAIXA NEGRA
        Swal.fire({
          icon: 'error',
          title: 'Erro ao guardar',
          text: e.error?.message || 'Verifica os dados e tenta novamente.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}