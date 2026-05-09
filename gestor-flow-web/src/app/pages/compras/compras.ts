import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CompraService } from '../../services/compra.service';
import { ArtigoService } from '../../services/artigo.service';
import { FornecedorService } from '../../services/fornecedor.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 

import { Compra, TaxaIva } from '../../core/models/compra.model'; // 🛡️ ADICIONADO TaxaIva
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
  templateUrl: './compras.html'
})
export class ComprasComponent implements OnInit {

  listaCompras: Compra[] = [];
  listaArtigos: Artigo[] = [];
  listaFornecedores: Fornecedor[] = [];
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];
  listaTaxasIva: TaxaIva[] = []; // 🛡️ MUDADO DE any[] para TaxaIva[]

  formCompra!: FormGroup;
  totalCalculado: number = 0;
  
  // 🚀 A MEMÓRIA DO NOSSO COMPONENTE (Para sabermos se é Update ou Create)
  compraEmEdicao: Compra | null = null;

  constructor(
    private compraService: CompraService,
    private artigoService: ArtigoService,
    private fornecedorService: FornecedorService,
    private analiticaService: AnaliticaService,
    private tesourariaService: TesourariaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarTudo();
    this.formCompra.valueChanges.subscribe(() => this.calcularTotal());

    this.compraService.compras$.subscribe((comprasAtualizadas) => {
      this.listaCompras = comprasAtualizadas;
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formCompra = this.fb.group({
      dataCompra: [this.getDataAtual(), [Validators.required]],
      dataVencimento: [this.getDataAtual(), [Validators.required]], // 🚀 NOVO CAMPO ADICIONADO!
      fornecedorId: [null, [Validators.required]],
      artigoId: [null, [Validators.required]],
      taxaIvaId: [null, [Validators.required]], 
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      precoUnitario: [null, [Validators.required, Validators.min(0)]],
      numeroFaturaFornecedor: [''],
      designacaoPersonalizada: [''],
      centroCustoId: [null, [Validators.required]], 
      seccaoHomoId: [null, [Validators.required]]
    });
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  // 🛡️ NOVO UTILITÁRIO: Para as datas não rebentarem ao editar
  formatarDataParaInput(dataIso: string | undefined): string {
    if (!dataIso) return this.getDataAtual();
    const d = new Date(dataIso);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
  }

  get f() { return this.formCompra.controls; }

  carregarTudo() {
    this.compraService.carregarComprasDaAPI();

    forkJoin({
      artigos: this.artigoService.listar(),
      fornecedores: this.fornecedorService.listar(),
      centros: this.analiticaService.listarCentros(),
      seccoes: this.analiticaService.listarSeccoes(),
      taxas: this.compraService.listarTaxasIva()
    }).subscribe({
      next: (res) => {
        this.listaArtigos = res.artigos.content || res.artigos;
        this.listaFornecedores = res.fornecedores;
        this.listaCentros = res.centros;
        this.listaSeccoes = res.seccoes;
        this.listaTaxasIva = res.taxas;
        this.cd.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar dados das compras:', err)
    });
  }

  aoSelecionarArtigo() {
    // Só limpar o preço se NÃO estivermos no meio de uma edição a preencher dados antigos!
    if (!this.compraEmEdicao) {
        this.formCompra.patchValue({ precoUnitario: null });
    }
  }

  calcularTotal() {
    const qtd = this.formCompra.get('quantidade')?.value || 0;
    const preco = this.formCompra.get('precoUnitario')?.value || 0;
    const taxaId = this.formCompra.get('taxaIvaId')?.value;
    
    let taxaValor = 0;
    if (taxaId) {
        const taxaObj = this.listaTaxasIva.find(t => t.id == taxaId);
        if (taxaObj) taxaValor = taxaObj.valor;
    }
    this.totalCalculado = (qtd * preco) * (1 + (taxaValor / 100));
  }

  abrirModalNovo() {
    this.compraEmEdicao = null; // 🚀 Garante que é uma fatura nova
    this.formCompra.reset({ 
        dataCompra: this.getDataAtual(), 
        dataVencimento: this.getDataAtual(), // 🚀 NOVO CAMPO NO RESET
        quantidade: 1, 
        precoUnitario: null,
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null
    });
    this.totalCalculado = 0;
    const modal = new bootstrap.Modal(document.getElementById('modalCompra'));
    modal.show();
  }

  // =========================================================================
  // --- 🚀 NOVAS FUNÇÕES INDUSTRIAIS: EDITAR E ELIMINAR NO ECRÃ ---
  // =========================================================================

  abrirModalEditar(compra: Compra) {
    // 🛡️ A REGRA DO DINHEIRO TAMBÉM VIVE NO FRONTEND
    if (compra.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Operação Bloqueada', text: 'Não é possível editar uma fatura que já tenha sido paga. Estorne o pagamento na Tesouraria primeiro.', confirmButtonColor: '#0d6efd'});
      return;
    }

    this.compraEmEdicao = compra;

    // Injetar os valores todos para dentro do formulário (Usando a nova formatação)
    this.formCompra.patchValue({
      dataCompra: this.formatarDataParaInput(compra.dataCompra),
      dataVencimento: this.formatarDataParaInput(compra.dataVencimento), // 🚀 INJEÇÃO DO VENCIMENTO
      fornecedorId: compra.fornecedorId,
      artigoId: compra.artigoId,
      taxaIvaId: compra.taxaIvaId,
      quantidade: compra.quantidade,
      precoUnitario: compra.precoUnitario,
      numeroFaturaFornecedor: compra.numeroFaturaFornecedor,
      designacaoPersonalizada: compra.designacao,
      centroCustoId: compra.centroCustoId,
      seccaoHomoId: compra.seccaoHomoId
    });

    this.calcularTotal();

    const modal = new bootstrap.Modal(document.getElementById('modalCompra')!);
    modal.show();
  }

  eliminarCompra(compra: Compra) {
    if (compra.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Operação Bloqueada', text: 'Não é possível eliminar uma fatura que já tenha sido paga. Estorne o pagamento na Tesouraria primeiro.', confirmButtonColor: '#0d6efd'});
      return;
    }

    Swal.fire({
      title: 'Tem a certeza?',
      text: `Vai anular a fatura de ${compra.artigoNome} do fornecedor ${compra.fornecedorNome}. O stock será retirado do armazém!`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Sim, anular e retirar stock!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.compraService.eliminar(compra.id!).subscribe({
          next: () => {
            Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Compra eliminada e stock reposto!'});
            // Opcional: Atualizar pendentes da tesouraria
            this.tesourariaService.notificarNovaTransacao(); 
          },
          error: (e) => {
            Swal.fire('Erro Interno', e.error?.message || 'Falha ao anular a fatura.', 'error');
          }
        });
      }
    });
  }

  guardarCompra() {
    if (this.formCompra.invalid) {
      this.formCompra.markAllAsTouched();
      return;
    }

    // 🚀 O BOTÃO INTELIGENTE: Se tivermos compra em edição, é um UPDATE. Se não, é REGISTO.
    const operacao$ = this.compraEmEdicao 
        ? this.compraService.atualizar(this.compraEmEdicao.id!, this.formCompra.value)
        : this.compraService.registar(this.formCompra.value);
    
    operacao$.subscribe({
      next: () => {
        const Toast = Swal.mixin({
          toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
        });
        Toast.fire({ 
            icon: 'success', 
            title: this.compraEmEdicao ? 'Fatura atualizada com sucesso!' : 'Compra registada com sucesso!' 
        });
        
        // Se mudámos valores, a tesouraria precisa de saber!
        this.tesourariaService.notificarNovaTransacao();
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalCompra'));
        modal?.hide();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: this.compraEmEdicao ? 'Falha ao Atualizar' : 'Falha ao Registar',
          text: e.error?.message || 'Verifica os dados inseridos e tenta novamente.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}