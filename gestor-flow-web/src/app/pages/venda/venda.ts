import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
// Serviços
import { VendaService } from '../../services/venda.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { AnaliticaService } from '../../services/analitica.service';
// Modelos
import { Venda } from '../../core/models/venda.model';
import { Artigo } from '../../core/models/artigo.model';
import { Cliente } from '../../core/models/cliente.model';
import { CentroCusto, SeccaoHomo } from '../../core/models/analitica.model';

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
  
  // Analítica (Opcional na venda, mas bom ter)
  listaCentros: CentroCusto[] = [];
  listaSeccoes: SeccaoHomo[] = [];

  formVenda!: FormGroup;
  totalCalculado: number = 0;
  stockDisponivel: number | null = null; // Para mostrar aviso no modal

  constructor(
    private vendaService: VendaService,
    private artigoService: ArtigoService,
    private clienteService: ClienteService,
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarTudo();
    
    // Recalcular total sempre que algo muda
    this.formVenda.valueChanges.subscribe(() => this.calcularTotal());
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
      centroCustoId: [null, [Validators.required]], // Nas vendas costuma ser menos rígido, mas podes pôr required
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

  get f() { return this.formVenda.controls; }

  carregarTudo() {
    this.vendaService.listar().subscribe(d => {
       this.listaVendas = d.content || d; 
       this.cd.detectChanges(); 
    });
    this.artigoService.listar().subscribe(d => this.listaArtigos = d.content || d);
    this.clienteService.listar().subscribe(d => this.listaClientes = d.content || d); // Confirma se o teu ClienteService devolve lista ou paginação
    this.vendaService.listarTaxasIva().subscribe(d => this.listaTaxasIva = d);
    
    this.analiticaService.listarCentros().subscribe(d => this.listaCentros = d);
    this.analiticaService.listarSeccoes().subscribe(d => this.listaSeccoes = d);
  }

  aoSelecionarArtigo() {
    const artigoId = this.formVenda.get('artigoId')?.value;
    const artigo = this.listaArtigos.find(a => a.id == artigoId);
    
    if (artigo) {
      // 1. Sugerir Preço de Venda
      this.formVenda.patchValue({ precoUnitario: artigo.preco });
      
      // 2. Mostrar Stock Disponível
      this.stockDisponivel = artigo.stockAtual || 0;

      // 3. Validar se a quantidade pedida excede o stock (visual apenas)
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

    // Fórmula: (Qtd * Preço) * (1 + Taxa/100)
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
    const modal = new bootstrap.Modal(document.getElementById('modalVenda'));
    modal.show();
  }

  registarVenda() {
    if (this.formVenda.invalid) {
      this.formVenda.markAllAsTouched();
      return;
    }
    
    // Validação extra de stock no frontend antes de enviar
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
        this.carregarTudo();
        const modal = bootstrap.Modal.getInstance(document.getElementById('modalVenda'));
        modal?.hide();
      },
      error: (e) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }
}