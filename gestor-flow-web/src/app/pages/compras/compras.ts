import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CompraService } from '../../services/compra.service';
import { ArtigoService } from '../../services/artigo.service';
import { FornecedorService } from '../../services/fornecedor.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 

import { Compra } from '../../core/models/compra.model';
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
  // ❌ seccoesFiltradas removida
  listaTaxasIva: any[] = []; 

  formCompra!: FormGroup;
  totalCalculado: number = 0;

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
    
    // ❌ Subscrição do centroCustoId removida

    this.compraService.compras$.subscribe((comprasAtualizadas) => {
      this.listaCompras = comprasAtualizadas;
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formCompra = this.fb.group({
      dataCompra: [this.getDataAtual(), [Validators.required]],
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

  // ❌ Função filtrarSeccoes removida

  aoSelecionarArtigo() {
    this.formCompra.patchValue({ precoUnitario: null });
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
    this.formCompra.reset({ 
        dataCompra: this.getDataAtual(), 
        quantidade: 1, 
        precoUnitario: null,
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null
    });
    this.totalCalculado = 0;
    // ❌ Limpeza da array seccoesFiltradas removida
    const modal = new bootstrap.Modal(document.getElementById('modalCompra'));
    modal.show();
  }

  registarCompra() {
    if (this.formCompra.invalid) {
      this.formCompra.markAllAsTouched();
      return;
    }
    
    this.compraService.registar(this.formCompra.value).subscribe({
      next: () => {
        const Toast = Swal.mixin({
          toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
        });
        Toast.fire({ icon: 'success', title: 'Compra registada com sucesso!' });
        
        this.tesourariaService.notificarNovaTransacao();
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalCompra'));
        modal?.hide();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: 'Falha ao Registar',
          text: e.error?.message || 'Verifica os dados inseridos e tenta novamente.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}