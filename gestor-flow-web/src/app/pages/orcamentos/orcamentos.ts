import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { OrcamentoService } from '../../services/orcamento.service';
import { ClienteService } from '../../services/cliente.service';
import { ArtigoService } from '../../services/artigo.service';
import { VendaService } from '../../services/venda.service'; // Para ir buscar as Taxas IVA
import { Orcamento } from '../../core/models/orcamento.model';

declare var bootstrap: any;

@Component({
  selector: 'app-orcamentos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './orcamentos.html'
})
export class OrcamentosComponent implements OnInit {

  listaOrcamentos: Orcamento[] = [];
  listaClientes: any[] = [];
  listaArtigos: any[] = [];
  listaTaxasIva: any[] = [];
  
  formOrcamento!: FormGroup;
  idEmEdicao: number | null = null;
  totalGeralPrevisto: number = 0;

  constructor(
    private orcamentoService: OrcamentoService,
    private clienteService: ClienteService,
    private artigoService: ArtigoService,
    private vendaService: VendaService, // Reutilizamos para ter as taxas
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDadosIniciais();
  }

  // Configuração do Formulário Mestre (Cabeçalho)
  inicializarFormulario() {
    this.formOrcamento = this.fb.group({
      clienteId: [null, Validators.required],
      dataValidade: [this.getDataDaquiA30Dias(), Validators.required],
      notas: [''],
      // O Array de Linhas (Começa vazio)
      linhas: this.fb.array([])
    });
  }

  // Helper para aceder às linhas no HTML
  get linhasForm(): FormArray {
    return this.formOrcamento.get('linhas') as FormArray;
  }

  carregarDadosIniciais() {
    this.orcamentoService.listar().subscribe(d => {
      this.listaOrcamentos = d.content || d;
      this.cd.detectChanges(); // <-- O SEGREDO ESTÁ AQUI! Força a tabela a atualizar
  });
    this.clienteService.listar().subscribe(d => this.listaClientes = d.content || d);
    this.artigoService.listar().subscribe(d => this.listaArtigos = d.content || d);
    this.vendaService.listarTaxasIva().subscribe(d => this.listaTaxasIva = d);
  }

  // --- GESTÃO DE LINHAS ---

  adicionarLinha(itemPreenchido: any = null) {
    const linha = this.fb.group({
      artigoId: [itemPreenchido?.artigoId || null, Validators.required],
      quantidade: [itemPreenchido?.quantidade || 1, [Validators.required, Validators.min(0.01)]],
      taxaIvaId: [itemPreenchido?.taxaIvaId || (this.listaTaxasIva[0]?.id), Validators.required],
      
      // O utilizador escolhe UM destes dois:
      margemLucroPercentual: [itemPreenchido?.margemLucroPercentual || 30], // Default 30% lucro
      precoVendaUnitarioOverride: [itemPreenchido?.precoVendaUnitarioOverride || null], // Preço manual
    });

    this.linhasForm.push(linha);
    this.calcularTotais();
  }

  removerLinha(index: number) {
    this.linhasForm.removeAt(index);
    this.calcularTotais();
  }

  // Chamado sempre que o utilizador muda artigo, qtd ou margem
  calcularTotais() {
    this.totalGeralPrevisto = 0;

    this.linhasForm.controls.forEach((group: any) => {
        const artigoId = group.get('artigoId').value;
        const qtd = group.get('quantidade').value || 0;
        const margem = group.get('margemLucroPercentual').value || 0;
        const precoManual = group.get('precoVendaUnitarioOverride').value;
        const taxaId = group.get('taxaIvaId').value;

        // Buscar Custo e Taxa
        const artigo = this.listaArtigos.find(a => a.id == artigoId);
        const taxa = this.listaTaxasIva.find(t => t.id == taxaId);
        
        const custo = artigo?.ultimoPrecoCusto || 0;
        const valorTaxa = taxa?.valor || 0;

        let precoFinalUnitario = 0;

        if (precoManual) {
            // Se escreveu preço manual, usa esse
            precoFinalUnitario = precoManual;
        } else {
            // Senão, calcula: Custo + Margem
            precoFinalUnitario = custo * (1 + (margem / 100));
        }

        const totalLinha = (precoFinalUnitario * qtd) * (1 + (valorTaxa / 100));
        this.totalGeralPrevisto += totalLinha;
    });
  }

  // --- AÇÕES CRUD ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.inicializarFormulario();
    this.adicionarLinha(); // Adiciona uma linha vazia para começar
    this.totalGeralPrevisto = 0;
    new bootstrap.Modal(document.getElementById('modalOrcamento')).show();
  }

  editar(orcamento: Orcamento) {
    this.idEmEdicao = orcamento.id!;
    this.inicializarFormulario();
    
    // Preencher Cabeçalho
    this.formOrcamento.patchValue({
        clienteId: orcamento.cliente?.id,
        dataValidade: orcamento.dataValidade,
        notas: orcamento.notas
    });

    // Preencher Linhas (Importante!)
    orcamento.linhas.forEach(linha => {
        this.adicionarLinha({
            artigoId: linha.artigo?.id,
            quantidade: linha.quantidade,
            taxaIvaId: linha.taxaIva?.id,
            margemLucroPercentual: linha.margemLucroPercentual,
            precoVendaUnitarioOverride: null // Ao editar, assumimos o calculo da margem para simplificar
        });
    });

    this.calcularTotais();
    new bootstrap.Modal(document.getElementById('modalOrcamento')).show();
  }

  guardar() {
    if (this.formOrcamento.invalid) {
      this.formOrcamento.markAllAsTouched();
      alert('Preenche todos os campos obrigatórios.');
      return;
    }

    const dto = this.formOrcamento.value;

    if (this.idEmEdicao) {
        this.orcamentoService.atualizar(this.idEmEdicao, dto).subscribe(() => this.fecharModal());
    } else {
        this.orcamentoService.criar(dto).subscribe(() => this.fecharModal());
    }
  }

  // --- A MÁGICA: CONVERTER ---
  converterEmVenda(id: number) {
    if(!confirm('Tem a certeza? Isto vai criar as vendas, abater stock e fechar o orçamento.')) return;

    this.orcamentoService.converterEmVenda(id).subscribe({
        next: () => {
            alert('Sucesso! Vendas geradas e stock atualizado.');
            this.carregarDadosIniciais();
        },
        error: (e) => alert('Erro: ' + e.error?.message)
    });
  }

  fecharModal() {
    bootstrap.Modal.getInstance(document.getElementById('modalOrcamento'))?.hide();
    this.carregarDadosIniciais();
  }

  // Utils
  getDataDaquiA30Dias(): string {
    const data = new Date();
    data.setDate(data.getDate() + 30);
    return data.toISOString().split('T')[0];
  }
}