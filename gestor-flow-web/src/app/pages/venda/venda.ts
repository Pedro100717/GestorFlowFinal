import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { VendaService } from '../../services/venda.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 

import { Venda, TaxaIva } from '../../core/models/venda.model'; // 🛡️ Importado TaxaIva
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
  listaTaxasIva: TaxaIva[] = []; // 🛡️ Tipagem corrigida!
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];

  formVenda!: FormGroup;
  totalCalculado: number = 0;
  stockDisponivel: number | null = null; 
  
  vendaEmEdicao: Venda | null = null;

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
  }

  inicializarFormulario() {
    this.formVenda = this.fb.group({
      dataVenda: [this.getDataAtual(), [Validators.required]],
      // 🚀 NOVO CAMPO: O Motor do Simulador
      dataVencimento: [this.getDataAtual(), [Validators.required]], 
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

  // 🛡️ Função utilitária para converter datas do Java para o formato do input HTML
  formatarDataParaInput(dataIso: string | undefined): string {
    if (!dataIso) return this.getDataAtual();
    const d = new Date(dataIso);
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().slice(0, 16);
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
      error: (err) => console.error('Erro ao carregar dados:', err)
    });
  }

  aoSelecionarArtigo() {
    if (!this.vendaEmEdicao) {
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
    this.vendaEmEdicao = null;
    this.formVenda.reset({ 
        dataVenda: this.getDataAtual(),
        dataVencimento: this.getDataAtual(),
        quantidade: 1, 
        precoUnitario: 0,
        taxaIvaId: this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null
    });
    this.stockDisponivel = null;
    this.totalCalculado = 0;
    const modal = new bootstrap.Modal(document.getElementById('modalVenda'));
    modal.show();
  }

  abrirModalEditar(venda: Venda) {
    if (venda.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Operação Bloqueada', text: 'Estorne o pagamento primeiro.', confirmButtonColor: '#0d6efd'});
      return;
    }

    this.vendaEmEdicao = venda;
    const linhaBase = venda.linhas && venda.linhas.length > 0 ? venda.linhas[0] : null;

    let idIvaParaForm = null;
    if (linhaBase && linhaBase.taxaIvaValor) {
        const taxaCorreta = this.listaTaxasIva.find(t => t.valor === linhaBase.taxaIvaValor);
        if (taxaCorreta) idIvaParaForm = taxaCorreta.id;
    }

    this.formVenda.patchValue({
      dataVenda: this.formatarDataParaInput(venda.dataVenda),
      dataVencimento: this.formatarDataParaInput(venda.dataVencimento), // 🚀 Carrega o vencimento
      clienteId: venda.clienteId,
      centroCustoId: venda.centroCustoId,
      seccaoHomoId: venda.seccaoHomoId,
      artigoId: linhaBase ? linhaBase.artigoId : null,
      taxaIvaId: idIvaParaForm,
      quantidade: linhaBase ? linhaBase.quantidade : 1,
      precoUnitario: linhaBase ? linhaBase.precoUnitario : 0,
      designacaoPersonalizada: linhaBase ? linhaBase.designacaoPersonalizada : ''
    });

    this.calcularTotal();
    const modal = new bootstrap.Modal(document.getElementById('modalVenda')!);
    modal.show();
  }

  eliminarVenda(venda: Venda) {
    if (venda.estadoPagamento !== 'PENDENTE') {
      Swal.fire({ icon: 'warning', title: 'Operação Bloqueada', text: 'Não é possível eliminar uma fatura já recebida.', confirmButtonColor: '#0d6efd'});
      return;
    }

    Swal.fire({
      title: 'Tem a certeza?',
      text: `Vai anular a fatura de ${venda.clienteNome}.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      confirmButtonText: 'Sim, anular!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.vendaService.anular(venda.id!).subscribe({
          next: () => {
            Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Anulada!'});
            this.tesourariaService.notificarNovaTransacao(); 
          },
          error: (e) => Swal.fire('Erro', e.error?.message, 'error')
        });
      }
    });
  }

  guardarVenda() {
    if (this.formVenda.invalid) {
      this.formVenda.markAllAsTouched();
      return;
    }
    this.executarRegistoOuUpdate();
  }

  private executarRegistoOuUpdate() {
    const formVal = this.formVenda.value;
    
    const payloadVenda = {
        dataVenda: formVal.dataVenda,
        dataVencimento: formVal.dataVencimento, // 🚀 ENVIAR PARA O JAVA
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

    const operacao$ = this.vendaEmEdicao 
        ? this.vendaService.atualizar(this.vendaEmEdicao.id!, payloadVenda)
        : this.vendaService.registar(payloadVenda);

    operacao$.subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Guardado!' });
        this.tesourariaService.notificarNovaTransacao(); 
        bootstrap.Modal.getInstance(document.getElementById('modalVenda'))?.hide();
      },
      error: (e: any) => Swal.fire('Erro', e.error?.message, 'error')
    });
  }
}