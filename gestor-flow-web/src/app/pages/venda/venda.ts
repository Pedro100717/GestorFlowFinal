import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VendaService } from '../../services/venda.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 

import { Venda } from '../../core/models/venda.model';
import { Artigo } from '../../core/models/artigo.model';
import { Cliente } from '../../core/models/cliente.model';
import { CentroCusto, SeccaoHomo } from '../../core/models/analitica.model';
import { forkJoin } from 'rxjs';

declare var bootstrap: any;

@Component({
  selector: 'app-vendas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './venda.html'
})
export class VendasComponent implements OnInit {

  listaVendas: Venda[] = [];
  listaArtigos: Artigo[] = [];
  listaClientes: Cliente[] = [];
  listaTaxasIva: any[] = [];
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];
  listaContas: any[] = []; 

  formVenda!: FormGroup;
  totalCalculado: number = 0;
  stockDisponivel: number | null = null; 

  constructor(
    private vendaService: VendaService,
    private artigoService: ArtigoService,
    private clienteService: ClienteService,
    private analiticaService: AnaliticaService,
    private tesourariaService: TesourariaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarTudo();
    this.formVenda.valueChanges.subscribe(() => this.calcularTotal());
    
    this.vendaService.vendas$.subscribe((vendasAtualizadas) => {
      this.listaVendas = vendasAtualizadas;
      this.cd.detectChanges();
    });

    // --- A MÁGICA REATIVA ---
    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formVenda = this.fb.group({
      dataVenda: [this.getDataAtual(), [Validators.required]],
      clienteId: [null, [Validators.required]],
      artigoId: [null, [Validators.required]],
      taxaIvaId: [null, [Validators.required]],
      contaBancariaId: [null, [Validators.required]], 
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      precoUnitario: [0, [Validators.required, Validators.min(0)]],
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

  get f() { return this.formVenda.controls; }

  carregarTudo() {
    this.vendaService.carregarVendasDaAPI();
    this.tesourariaService.carregarContasDaAPI();

    forkJoin({
      artigos: this.artigoService.listar(),
      clientes: this.clienteService.listar(),
      centros: this.analiticaService.listarCentros(),
      seccoes: this.analiticaService.listarSeccoes(),
      taxas: this.vendaService.listarTaxasIva()
    }).subscribe({
      next: (res) => {
        this.listaArtigos = res.artigos.content || res.artigos;
        this.listaClientes = res.clientes.content || res.clientes;
        this.listaCentros = res.centros;
        this.listaSeccoes = res.seccoes;
        this.listaTaxasIva = res.taxas;
        this.cd.detectChanges();
      },
      error: (err) => console.error('Erro ao carregar dados das vendas:', err)
    });
  }

  aoSelecionarArtigo() {
    const artigoId = this.formVenda.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);
    
    if (artigo) {
      this.formVenda.patchValue({ precoUnitario: artigo.preco });
      this.stockDisponivel = artigo.stockAtual || 0;
      if (artigo.movimentaStock && this.stockDisponivel! <= 0) {
          alert('Atenção: Este artigo não tem stock disponível!');
      }
    } else {
        this.stockDisponivel = null;
    }
  }

  calcularTotal() {
    const qtd = this.formVenda.get('quantidade')?.value || 0;
    const preco = this.formVenda.get('precoUnitario')?.value || 0;
    const taxaId = this.formVenda.get('taxaIvaId')?.value;
    
    let taxaValor = 0;
    if (taxaId) {
        const taxaObj = this.listaTaxasIva.find(t => t.id == taxaId);
        if (taxaObj) taxaValor = taxaObj.valor;
    }
    this.totalCalculado = (qtd * preco) * (1 + (taxaValor / 100));
  }

  abrirModalNovo() {
    this.formVenda.reset({ 
        dataVenda: this.getDataAtual(),
        quantidade: 1, 
        precoUnitario: 0,
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null,
        contaBancariaId: null 
    });
    this.stockDisponivel = null;
    this.totalCalculado = 0;
    const modal = new bootstrap.Modal(document.getElementById('modalVenda'));
    modal.show();
  }

  registarVenda() {
    if (this.formVenda.invalid) {
      this.formVenda.markAllAsTouched();
      return;
    }
    
    const qtd = this.formVenda.get('quantidade')?.value;
    const artigoId = this.formVenda.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);

    if (artigo && artigo.movimentaStock && (artigo.stockAtual || 0) < qtd) {
        if(!confirm(`Tens apenas ${artigo.stockAtual} em stock. Queres vender ${qtd} mesmo assim (stock ficará negativo)?`)) {
            return;
        }
    }

    this.vendaService.registar(this.formVenda.value).subscribe({
      next: () => {
        alert('Venda registada com sucesso!');
        
        // Avisa a tesouraria
        this.tesourariaService.notificarNovaTransacao(); 
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalVenda'));
        modal?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }
}