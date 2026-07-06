import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { Router } from '@angular/router'; 

import { VendaService } from '../../services/venda.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { AnaliticaService } from '../../services/analitica.service';
import { TesourariaService } from '../../services/tesouraria.service'; 

import { Venda, LinhaVenda, TaxaIva } from '../../core/models/venda.model'; 
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
export class VendasComponent implements OnInit, AfterViewInit {

  listaVendas: Venda[] = [];
  listaArtigos: Artigo[] = [];
  listaClientes: Cliente[] = [];
  listaTaxasIva: TaxaIva[] = []; 
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];

  formVenda!: FormGroup;
  totalCalculado: number = 0;
  
  vendaEmEdicao: Venda | null = null;

  // 🚀 VARIÁVEIS INVISÍVEIS PARA RECEBER O PLANO DA TESOURARIA
  planoOrigemId: number | null = null;
  planoOrigemDescricao: string = '';
  planoOrigemData: string | null = null;

  // 🚀 CONTROLO DE EXPANSÃO DE LINHAS NA TABELA
  faturasExpandidas = new Set<number>();

  constructor(
    private vendaService: VendaService,
    private artigoService: ArtigoService,
    private clienteService: ClienteService,
    private analiticaService: AnaliticaService,
    private tesourariaService: TesourariaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private router: Router
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
      Toast.fire({ icon: 'info', title: 'A preparar receita a partir do planeamento.' });
    }

    // 🚀 Recalcular o total geral sempre que qualquer linha sofrer alterações
    this.formVenda.get('linhas')?.valueChanges.subscribe(() => this.calcularTotalGeral());
    
    this.vendaService.vendas$.subscribe((vendasAtualizadas) => {
      this.listaVendas = vendasAtualizadas;
      this.cd.detectChanges();
    });
  }

  ngAfterViewInit() {
    if (this.planoOrigemId) {
      setTimeout(() => this.abrirModalNovo(), 500); 
    }
  }

  inicializarFormulario() {
    this.formVenda = this.fb.group({
      dataVenda: [this.getDataAtual(), [Validators.required]],
      dataVencimento: [this.getDataAtual(), [Validators.required]], 
      clienteId: [null, [Validators.required]],
      // 🚀 O MOTOR DE MÚLTIPLAS LINHAS
      linhas: this.fb.array([])
    });
  }

  // --- MÉTODOS DE EXPANSÃO DA TABELA ---

  toggleExpandir(id: number): void {
    if (this.faturasExpandidas.has(id)) {
      this.faturasExpandidas.delete(id);
    } else {
      this.faturasExpandidas.add(id);
    }
  }

  // --- MÉTODOS DO FORMARRAY ---

  get linhas(): FormArray {
    return this.formVenda.get('linhas') as FormArray;
  }

  criarLinha(dadosLinha?: LinhaVenda): FormGroup {
    let idIvaParaForm = null;
    if (dadosLinha && dadosLinha.taxaIvaValor) {
        const taxaCorreta = this.listaTaxasIva.find(t => t.valor === dadosLinha.taxaIvaValor);
        if (taxaCorreta) idIvaParaForm = taxaCorreta.id;
    } else {
        idIvaParaForm = this.listaTaxasIva.length > 0 ? this.listaTaxasIva[0].id : null;
    }

    return this.fb.group({
      centroCustoId: [dadosLinha?.centroCustoId || null, [Validators.required]],
      seccaoHomoId: [dadosLinha?.seccaoHomoId || null, [Validators.required]],
      artigoId: [dadosLinha?.artigoId || null, [Validators.required]],
      taxaIvaId: [idIvaParaForm, [Validators.required]],
      quantidade: [dadosLinha?.quantidade || 1, [Validators.required, Validators.min(0.001)]],
      precoUnitario: [dadosLinha?.precoUnitario || 0, [Validators.required, Validators.min(0)]],
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

  // --- REGRAS DE NEGÓCIO DA LINHA ---

  aoSelecionarArtigo(index: number) {
    if (!this.vendaEmEdicao) {
        const linha = this.linhas.at(index);
        const artigoId = linha.get('artigoId')?.value;
        const artigo = this.listaArtigos.find(a => a.id == artigoId);
        
        if (artigo) {
          linha.patchValue({ precoUnitario: artigo.preco });
          
          if (artigo.movimentaStock && (artigo.stockAtual || 0) <= 0) {
              const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 4000 });
              Toast.fire({ icon: 'warning', title: `Atenção: O artigo "${artigo.nome}" não tem stock disponível!` });
          }
        }
    }
  }

  // --- MATEMÁTICA ---

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

  // --- UTILITÁRIOS ---

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

  get f() { return this.formVenda.controls; }

  campoLinhaInvalido(index: number, campo: string): boolean {
    const control = this.linhas.at(index).get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

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

  // --- MODAL ---

  abrirModalNovo() {
    this.vendaEmEdicao = null;
    this.linhas.clear();
    this.adicionarLinha();

    this.formVenda.patchValue({ 
        dataVenda: this.getDataAtual(),
        dataVencimento: this.planoOrigemData ? this.formatarDataParaInput(this.planoOrigemData) : this.getDataAtual(),
        clienteId: null
    });

    if (this.planoOrigemDescricao) {
        this.linhas.at(0).patchValue({ designacaoPersonalizada: this.planoOrigemDescricao });
    }

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
    this.linhas.clear();
    
    if (venda.linhas && venda.linhas.length > 0) {
      venda.linhas.forEach(linhaBD => {
        this.linhas.push(this.criarLinha(linhaBD));
      });
    } else {
      this.adicionarLinha();
    }

    this.formVenda.patchValue({
      dataVenda: this.formatarDataParaInput(venda.dataVenda),
      dataVencimento: this.formatarDataParaInput(venda.dataVencimento), 
      clienteId: venda.clienteId
    });

    this.calcularTotalGeral();
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
      text: `Vai anular a fatura de ${venda.clienteNome}. O stock será reposto.`,
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
            
            // 🚀 AQUI ESTÁ: Atualiza o stock no ecrã porque a mercadoria foi devolvida!
            this.artigoService.listar().subscribe(res => {
                this.listaArtigos = res.content || res;
            });
          },
          error: (e) => Swal.fire('Erro', e.error?.message, 'error')
        });
      }
    });
  }

  guardarVenda() {
    if (this.formVenda.invalid) {
      this.formVenda.markAllAsTouched();
      this.linhas.controls.forEach(control => control.markAllAsTouched());
      Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'error', title: 'Preenche os campos obrigatórios nas linhas!'});
      return;
    }

    const payload = {
        ...this.formVenda.value,
        planoOrigemId: this.planoOrigemId ?? undefined 
    };

    const operacao$ = this.vendaEmEdicao 
        ? this.vendaService.atualizar(this.vendaEmEdicao.id!, payload)
        : this.vendaService.registar(payload);
    
    operacao$.subscribe({
      next: () => {
        Swal.fire({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, icon: 'success', title: 'Fatura guardada com sucesso!' });
        this.tesourariaService.notificarNovaTransacao(); 
        
        // 🚀 A SOLUÇÃO: Pede ao backend a lista de artigos fresca com o stock novo!
        this.artigoService.listar().subscribe(res => {
            this.listaArtigos = res.content || res;
        });
        
        this.planoOrigemId = null;
        this.planoOrigemDescricao = '';
        
        bootstrap.Modal.getInstance(document.getElementById('modalVenda'))?.hide();
      },
      error: (e: any) => Swal.fire('Erro ao Guardar', e.error?.message, 'error')
    });
  }
}