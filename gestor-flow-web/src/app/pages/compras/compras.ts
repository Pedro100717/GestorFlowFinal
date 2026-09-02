import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router'; 
import { HttpErrorResponse } from '@angular/common/http'; 

import { trigger, style, transition, animate } from '@angular/animations';

import { CompraService } from '../../services/compra.service';
import { ArtigoService } from '../../services/artigo.service';
import { FornecedorService } from '../../services/fornecedor.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 
import { IvaService } from '../../services/iva.service'; 
import { LogService } from '../../core/services/log.service'; // 🚀 IMPORT OBRIGATÓRIO DO INSPETOR

import { Compra, LinhaCompra, TaxaIva } from '../../core/models/compra.model'; 
import { Artigo } from '../../core/models/artigo.model';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { CentroCusto, SeccaoHomo } from '../../core/models/analitica.model';
import { forkJoin } from 'rxjs';

import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-compras',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './compras.html',
  styleUrl: './compras.scss', 
  animations: [
    trigger('expandirTabela', [
      transition(':enter', [
        style({ height: '0', opacity: 0, overflow: 'hidden' }),
        animate('300ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        style({ height: '*', opacity: 1, overflow: 'hidden' }),
        animate('250ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ height: '0', opacity: 0 }))
      ])
    ]),
    trigger('animarCartao', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-20px)' }),
        animate('300ms cubic-bezier(0.2, 0.8, 0.2, 1)', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ])
  ]
})
export class ComprasComponent implements OnInit, AfterViewInit {

  listaCompras: Compra[] = [];
  listaArtigos: Artigo[] = [];
  listaFornecedores: Fornecedor[] = [];
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];
  listaTaxasIva: TaxaIva[] = []; 

  formCompra!: FormGroup;
  totalCalculado: number = 0;
  
  compraEmEdicao: Compra | null = null;

  planoOrigemId: number | null = null;
  planoOrigemDescricao: string = '';
  planoOrigemData: string | null = null;

  faturasExpandidas: { [key: number]: boolean } = {};

  constructor(
    private compraService: CompraService,
    private artigoService: ArtigoService,
    private fornecedorService: FornecedorService,
    private analiticaService: AnaliticaService,
    private tesourariaService: TesourariaService,
    private ivaService: IvaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private router: Router,
    private logService: LogService 
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarTudo();

    const state = history.state;
    if (state && state.planoOrigemId) {
      this.planoOrigemId = state.planoOrigemId;
      this.planoOrigemDescricao = state.descricao;
      this.planoOrigemData = state.dataProjetada || null; 
      
      const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 4000 });
      Toast.fire({ icon: 'info', title: 'A preparar despesa a partir do planeamento.' });
    }

    this.formCompra.get('linhas')?.valueChanges.subscribe(() => this.calcularTotalGeral());

    this.compraService.compras$.subscribe((comprasAtualizadas) => {
      this.listaCompras = comprasAtualizadas;
      this.cd.detectChanges();
    });
  }

  ngAfterViewInit() {
    if (this.planoOrigemId) {
      setTimeout(() => this.abrirModalNovo(), 500); 
    }
  }

  inicializarFormulario() {
    this.formCompra = this.fb.group({
      dataCompra: [this.getDataAtual(), [Validators.required]],
      dataVencimento: [this.getDataAtual(), [Validators.required]], 
      fornecedorId: [null, [Validators.required]],
      numeroFaturaFornecedor: [''],
      linhas: this.fb.array([])
    });
  }

  toggleExpandir(id: number | undefined): void {
    if (!id) return;
    this.faturasExpandidas[id] = !this.faturasExpandidas[id];
  }

  get linhas(): FormArray {
    return this.formCompra.get('linhas') as FormArray;
  }

  criarLinha(dadosLinha?: LinhaCompra): FormGroup {
    return this.fb.group({
      artigoId: [dadosLinha?.artigoId || null, [Validators.required]],
      taxaIvaId: [dadosLinha?.taxaIvaId || (this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null), [Validators.required]],
      quantidade: [dadosLinha?.quantidade || 1, [Validators.required, Validators.min(0.001)]],
      precoUnitario: [dadosLinha?.precoUnitario || null, [Validators.required, Validators.min(0)]],
      centroCustoId: [dadosLinha?.centroCustoId || null, [Validators.required]],
      seccaoHomoId: [dadosLinha?.seccaoHomoId || null, [Validators.required]],
      designacaoPersonalizada: [dadosLinha?.designacaoPersonalizada || '']
    });
  }

  adicionarLinha() {
    this.linhas.push(this.criarLinha());
  }

  removerLinha(index: number) {
    if (this.linhas.length > 1) {
      this.linhas.removeAt(index);
    } else {
      Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'warning', title: 'A fatura tem de ter pelo menos uma linha!' });
    }
  }

  calcularTotalLinha(index: number): number {
    const linha = this.linhas.at(index);
    const qtd = linha.get('quantidade')?.value || 0;
    const preco = linha.get('precoUnitario')?.value || 0;
    const taxaId = linha.get('taxaIvaId')?.value;
    
    let taxaValor = 0;
    if (taxaId) {
        const taxaObj = this.listaTaxasIva.find(t => t.id == taxaId);
        if (taxaObj) taxaValor = taxaObj.valor;
    }
    return (qtd * preco) * (1 + (taxaValor / 100));
  }

  calcularTotalGeral() {
    let total = 0;
    for (let i = 0; i < this.linhas.length; i++) {
      total += this.calcularTotalLinha(i);
    }
    this.totalCalculado = total;
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 10); 
  }

  formatarDataParaInput(dataIso: string | undefined): string {
    if (!dataIso) return this.getDataAtual();
    const d = new Date(dataIso);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 10); 
  }

  get f() { return this.formCompra.controls; }

  campoLinhaInvalido(index: number, campo: string): boolean {
    const control = this.linhas.at(index).get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  carregarTudo() {
    this.compraService.carregarComprasDaAPI();

    forkJoin({
      artigos: this.artigoService.listar(),
      fornecedores: this.fornecedorService.listar(),
      centros: this.analiticaService.listarCentros(),
      seccoes: this.analiticaService.listarSeccoes(),
      taxas: this.ivaService.listar() 
    }).subscribe({
      next: (res) => {
        this.listaArtigos = res.artigos.content || [];
        this.listaFornecedores = res.fornecedores.content || []; 
        this.listaCentros = res.centros;
        this.listaSeccoes = res.seccoes;
        this.listaTaxasIva = res.taxas;
        this.cd.detectChanges();
        this.logService.debug('Dados auxiliares de Compras carregados com sucesso.'); 
      },
      error: (err: HttpErrorResponse) => this.logService.error('Erro ao carregar dados auxiliares de Compras', err) 
    });
  }

  abrirModalNovo() {
    this.compraEmEdicao = null; 
    
    this.linhas.clear();
    this.adicionarLinha();

    this.formCompra.patchValue({     
        dataCompra: this.getDataAtual(), 
        dataVencimento: this.planoOrigemData ? this.formatarDataParaInput(this.planoOrigemData) : this.getDataAtual(), 
        fornecedorId: null,
        numeroFaturaFornecedor: ''
    });

    if (this.planoOrigemDescricao) {
        this.linhas.at(0).patchValue({ designacaoPersonalizada: this.planoOrigemDescricao });
    }

    this.totalCalculado = 0;
    const modal = new bootstrap.Modal(document.getElementById('modalCompra'));
    modal.show();
  }

  abrirModalEditar(compra: Compra) {
    if (compra.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Bloqueado', text: 'Não é possível editar uma fatura paga.', confirmButtonColor: '#0d6efd'});
      return;
    }

    this.compraEmEdicao = compra;

    this.linhas.clear();
    if (compra.linhas && compra.linhas.length > 0) {
      compra.linhas.forEach(linhaBD => {
        this.linhas.push(this.criarLinha(linhaBD));
      });
    } else {
      this.adicionarLinha(); 
    }

    this.formCompra.patchValue({
      dataCompra: this.formatarDataParaInput(compra.dataCompra),
      dataVencimento: this.formatarDataParaInput(compra.dataVencimento), 
      fornecedorId: compra.fornecedorId,
      numeroFaturaFornecedor: compra.numeroFaturaFornecedor
    });

    this.calcularTotalGeral();

    const modal = new bootstrap.Modal(document.getElementById('modalCompra')!);
    modal.show();
  }

  eliminarCompra(compra: Compra) {
    if (compra.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Bloqueado', text: 'Não é possível eliminar uma fatura paga.', confirmButtonColor: '#0d6efd'});
      return;
    }

    Swal.fire({
      title: 'Tem a certeza?',
      text: `Vai anular a fatura do fornecedor ${compra.fornecedorNome}. O stock dos artigos será retirado!`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sim, anular!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.compraService.eliminar(compra.id!).subscribe({
          next: () => {
            this.logService.info(`Fatura de compra ${compra.id} eliminada com sucesso.`); 
            Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Fatura eliminada e stock reposto!'});
            this.tesourariaService.notificarNovaTransacao(); 
            
            this.artigoService.listar().subscribe(res => {
                this.listaArtigos = res.content || [];
            });
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Falha ao anular fatura de compra ${compra.id}`, e); 
            Swal.fire('Erro Interno', e.error?.message || 'Falha ao anular.', 'error'); 
          }
        });
      }
    });
  }

  guardarCompra() {
    // 🚀 CORREÇÃO P1-05: Se o formulário for inválido, em vez de bloquear silenciosamente, avisa o utilizador.
    if (this.formCompra.invalid || this.linhas.length === 0) {
      this.formCompra.markAllAsTouched();
      this.linhas.controls.forEach(control => control.markAllAsTouched());
      
      let mensagem = 'Existem campos obrigatórios por preencher ou inválidos. Verifique os campos a vermelho.';
      if (this.linhas.length === 0) {
        mensagem = 'Não pode guardar uma compra sem adicionar pelo menos uma linha de artigo.';
      }

      Swal.fire({ 
        icon: 'warning', 
        title: 'Formulário Incompleto', 
        text: mensagem,
        confirmButtonColor: '#0d6efd'
      });
      return;
    }

    const payload = {
        ...this.formCompra.value,
        planoOrigemId: this.planoOrigemId ?? undefined 
    };

    const operacao$ = this.compraEmEdicao 
        ? this.compraService.atualizar(this.compraEmEdicao.id!, payload)
        : this.compraService.registar(payload);
    
    operacao$.subscribe({
      next: () => {
        this.logService.debug(this.compraEmEdicao ? `Fatura de compra ${this.compraEmEdicao.id} atualizada.` : 'Nova fatura de compra registada.'); 
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: this.compraEmEdicao ? 'Atualizado com sucesso!' : 'Registado com sucesso!' });
        this.tesourariaService.notificarNovaTransacao();
        
        this.artigoService.listar().subscribe(res => {
            this.listaArtigos = res.content || [];
        });

        this.planoOrigemId = null;
        this.planoOrigemDescricao = '';
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalCompra'));
        modal?.hide();
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Falha ao guardar fatura de compra', e); 
        Swal.fire({ icon: 'error', title: 'Falha a gravar', text: e.error?.message, confirmButtonColor: '#0d6efd'}); 
      }
    });
  }
}