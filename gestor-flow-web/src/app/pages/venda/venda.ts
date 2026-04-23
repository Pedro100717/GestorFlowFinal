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

import Swal from 'sweetalert2';

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
  // ❌ seccoesFiltradas removida - as secções agora são livres e independentes!

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
    
    // ❌ Subscrição do centroCustoId removida! Já não há filtro de secções por centro.
    
    this.vendaService.vendas$.subscribe((vendasAtualizadas) => {
      this.listaVendas = vendasAtualizadas;
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formVenda = this.fb.group({
      dataVenda: [this.getDataAtual(), [Validators.required]],
      clienteId: [null, [Validators.required]],
      artigoId: [null, [Validators.required]],
      taxaIvaId: [null, [Validators.required]],
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

  // ❌ Função filtrarSeccoes() removida na íntegra!

  aoSelecionarArtigo() {
    const artigoId = this.formVenda.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);
    
    if (artigo) {
      this.formVenda.patchValue({ precoUnitario: artigo.preco });
      this.stockDisponivel = artigo.stockAtual || 0;
      
      if (artigo.movimentaStock && this.stockDisponivel! <= 0) {
          const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 4000 });
          Toast.fire({ icon: 'warning', title: 'Atenção: Este artigo não tem stock disponível!' });
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
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null
    });
    this.stockDisponivel = null;
    this.totalCalculado = 0;
    // ❌ Limpeza da array seccoesFiltradas removida
    const modal = new bootstrap.Modal(document.getElementById('modalVenda'));
    modal.show();
  }

  registarVenda() {
    if (this.formVenda.invalid) {
      this.formVenda.markAllAsTouched();
      Swal.fire({
        icon: 'warning',
        title: 'Atenção',
        text: 'Por favor, preencha todos os campos obrigatórios corretamente.',
        confirmButtonColor: '#0d6efd'
      });
      return;
    }
    
    const qtd = this.formVenda.get('quantidade')?.value;
    const artigoId = this.formVenda.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);

    if (artigo && artigo.movimentaStock && (artigo.stockAtual || 0) < qtd) {
        Swal.fire({
          title: 'Stock Insuficiente',
          text: `Tens apenas ${artigo.stockAtual} em stock. Queres vender ${qtd} mesmo assim (o stock ficará negativo)?`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonColor: '#f39c12',
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sim, vender mesmo assim!',
          cancelButtonText: 'Cancelar'
        }).then((result) => {
          if (result.isConfirmed) {
            this.executarRegistoVenda();
          }
        });
    } else {
        this.executarRegistoVenda();
    }
  }

  private executarRegistoVenda() {
    const formVal = this.formVenda.value;
    const payloadVenda = {
        dataVenda: formVal.dataVenda,
        clienteId: formVal.clienteId,
        centroCustoId: formVal.centroCustoId,
        seccaoHomoId: formVal.seccaoHomoId,
        linhas: [
            {
                artigoId: formVal.artigoId,
                quantidade: formVal.quantidade,
                precoUnitario: formVal.precoUnitario,
                taxaIvaId: formVal.taxaIvaId,
                designacaoPersonalizada: formVal.designacaoPersonalizada
            }
        ]
    };

    this.vendaService.registar(payloadVenda).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Venda registada com sucesso!' });
        
        this.tesourariaService.notificarNovaTransacao(); 
        
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalVenda'));
        modal?.hide();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: 'Erro ao registar',
          text: e.error?.message || 'Ocorreu um erro ao processar a venda.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}