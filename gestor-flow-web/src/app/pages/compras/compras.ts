import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CompraService } from '../../services/compra.service';
import { ArtigoService } from '../../services/artigo.service';
import { FornecedorService } from '../../services/fornecedor.service';
import { AnaliticaService } from '../../services/analitica.service';

import { Compra } from '../../core/models/compra.model';
import { Artigo } from '../../core/models/artigo.model';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { CentroCusto, SeccaoHomo } from '../../core/models/analitica.model';

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
  listaTaxasIva: any[] = []; // <--- NOVO

  formCompra!: FormGroup;

  // Variável auxiliar para mostrar o total calculado em tempo real
  totalCalculado: number = 0;

  constructor(
    private compraService: CompraService,
    private artigoService: ArtigoService,
    private fornecedorService: FornecedorService,
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarTudo();
    
    // Escutar mudanças para recalcular o total em tempo real
    this.formCompra.valueChanges.subscribe(() => this.calcularTotal());
  }

  inicializarFormulario() {
    this.formCompra = this.fb.group({
      dataCompra: [this.getDataAtual(), [Validators.required]], // Data e hora atual
      fornecedorId: [null, [Validators.required]],
      artigoId: [null, [Validators.required]],
      taxaIvaId: [null, [Validators.required]], // <--- NOVO: Obrigatório
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      precoUnitario: [0, [Validators.required, Validators.min(0)]],
      numeroFaturaFornecedor: [''],
      designacaoPersonalizada: [''],
      centroCustoId: [null, [Validators.required]], 
      seccaoHomoId: [null, [Validators.required]]
    });
  }

  // Gera a string "YYYY-MM-DDTHH:mm" necessária para o input datetime-local
  getDataAtual(): string {
    const now = new Date();
    // Ajuste de fuso horário simples para o input não ficar em UTC
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  get f() { return this.formCompra.controls; }

  carregarTudo() {
    this.compraService.listar().subscribe(d => {
       this.listaCompras = d.content || d; 
       this.cd.detectChanges(); 
    });
    this.artigoService.listar().subscribe(d => this.listaArtigos = d.content || d);
    this.fornecedorService.listar().subscribe(d => this.listaFornecedores = d);
    this.analiticaService.listarCentros().subscribe(d => this.listaCentros = d);
    this.analiticaService.listarSeccoes().subscribe(d => this.listaSeccoes = d);
    
    // Carregar as Taxas de IVA (usando o endpoint que já existe no ArtigoService)
    this.compraService.listarTaxasIva().subscribe(d => this.listaTaxasIva = d);
  }

  aoSelecionarArtigo() {
    const artigoId = this.formCompra.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);
    if (artigo) {
      // Sugere o último preço de custo (se existir), senão o preço de venda
      const precoSugerido = artigo.ultimoPrecoCusto || artigo.preco;
      this.formCompra.patchValue({ precoUnitario: precoSugerido });
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

    // Fórmula: (Qtd * Preço) * (1 + Taxa/100)
    this.totalCalculado = (qtd * preco) * (1 + (taxaValor / 100));
  }

  abrirModalNovo() {
    this.formCompra.reset({ 
        dataVenda: this.getDataAtual(),
        quantidade: 1, 
        precoUnitario: 0,
        // Define uma taxa padrão se houver (ex: a primeira da lista)
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null 
    });
    this.totalCalculado = 0;
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
        alert('Compra registada com sucesso!');
        this.carregarTudo();
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalCompra'));
        modal?.hide();
      },
      error: (e) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }
}