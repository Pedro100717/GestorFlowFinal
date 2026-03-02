import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators, FormsModule } from '@angular/forms';
import { OrcamentoService } from '../../services/orcamento.service';
import { ClienteService } from '../../services/cliente.service';
import { ArtigoService } from '../../services/artigo.service';
import { VendaService } from '../../services/venda.service'; 
import { TesourariaService } from '../../services/tesouraria.service'; 
import { Orcamento } from '../../core/models/orcamento.model';

declare var bootstrap: any;

@Component({
  selector: 'app-orcamentos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], 
  templateUrl: './orcamentos.html'
})
export class OrcamentosComponent implements OnInit {

  listaOrcamentos: Orcamento[] = [];
  listaClientes: any[] = [];
  listaArtigos: any[] = [];
  listaTaxasIva: any[] = [];
  listaContas: any[] = []; 
  
  formOrcamento!: FormGroup;
  idEmEdicao: number | null = null;
  totalGeralPrevisto: number = 0;

  orcamentoParaConverter: any = null;
  contaSelecionadaParaConversao: number | null = null;

  constructor(
    private orcamentoService: OrcamentoService,
    private clienteService: ClienteService,
    private artigoService: ArtigoService,
    private vendaService: VendaService,
    private tesourariaService: TesourariaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDadosIniciais();

    this.orcamentoService.orcamentos$.subscribe(lista => {
      this.listaOrcamentos = lista;
      this.cd.detectChanges();
    });

    // --- A MÁGICA REATIVA ---
    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formOrcamento = this.fb.group({
      clienteId: [null, Validators.required],
      contaBancariaId: [null], 
      dataValidade: [this.getDataDaquiA30Dias(), Validators.required],
      notas: [''],
      linhas: this.fb.array([])
    });
  }

  get linhasForm(): FormArray {
    return this.formOrcamento.get('linhas') as FormArray;
  }

  carregarDadosIniciais() {
    this.orcamentoService.carregarOrcamentosDaAPI();
    this.tesourariaService.carregarContasDaAPI(); // Enche o Cofre!

    this.clienteService.listar().subscribe(d => this.listaClientes = d.content || d);
    this.artigoService.listar().subscribe(d => this.listaArtigos = d.content || d);
    this.vendaService.listarTaxasIva().subscribe(d => this.listaTaxasIva = d);
  }

  adicionarLinha(itemPreenchido: any = null) {
    const linha = this.fb.group({
      artigoId: [itemPreenchido?.artigoId || null, Validators.required],
      quantidade: [itemPreenchido?.quantidade || 1, [Validators.required, Validators.min(0.01)]],
      taxaIvaId: [itemPreenchido?.taxaIvaId || (this.listaTaxasIva[0]?.id), Validators.required],
      margemLucroPercentual: [itemPreenchido?.margemLucroPercentual || 30],
      precoVendaUnitarioOverride: [itemPreenchido?.precoVendaUnitarioOverride || null], 
    });

    this.linhasForm.push(linha);
    this.calcularTotais();
  }

  removerLinha(index: number) {
    this.linhasForm.removeAt(index);
    this.calcularTotais();
  }

  calcularTotais() {
    this.totalGeralPrevisto = 0;

    this.linhasForm.controls.forEach((group: any) => {
        const artigoId = group.get('artigoId').value;
        const qtd = group.get('quantidade').value || 0;
        const margem = group.get('margemLucroPercentual').value || 0;
        const precoManual = group.get('precoVendaUnitarioOverride').value;
        const taxaId = group.get('taxaIvaId').value;

        const artigo = this.listaArtigos.find(a => a.id == artigoId);
        const taxa = this.listaTaxasIva.find(t => t.id == taxaId);
        
        const custo = artigo?.ultimoPrecoCusto || 0;
        const valorTaxa = taxa?.valor || 0;

        let precoFinalUnitario = 0;

        if (precoManual) {
            precoFinalUnitario = precoManual;
        } else {
            precoFinalUnitario = custo * (1 + (margem / 100));
        }

        const totalLinha = (precoFinalUnitario * qtd) * (1 + (valorTaxa / 100));
        this.totalGeralPrevisto += totalLinha;
    });
  }

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.inicializarFormulario();
    this.adicionarLinha(); 
    this.totalGeralPrevisto = 0;
    new bootstrap.Modal(document.getElementById('modalOrcamento')).show();
  }

  editar(orcamento: any) {
    this.idEmEdicao = orcamento.id!;
    this.inicializarFormulario();
    
    const contaId = orcamento.contaBancariaId || orcamento.contaBancaria?.id || null;

    this.formOrcamento.patchValue({
        clienteId: orcamento.cliente?.id || orcamento.clienteId,
        contaBancariaId: contaId,
        dataValidade: orcamento.dataValidade,
        notas: orcamento.notas
    });

    orcamento.linhas.forEach((linha: any) => {
        this.adicionarLinha({
            artigoId: linha.artigo?.id || linha.artigoId,
            quantidade: linha.quantidade,
            taxaIvaId: linha.taxaIva?.id || linha.taxaIvaId,
            margemLucroPercentual: linha.margemLucroPercentual,
            precoVendaUnitarioOverride: null
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

  prepararConversao(orcamento: any) {
    this.orcamentoParaConverter = orcamento;
    const contaIdOriginal = orcamento.contaBancariaId || orcamento.contaBancaria?.id;

    if (contaIdOriginal) {
        if(confirm('Este orçamento já tem uma conta associada. Pretende faturar agora?')) {
            this.executarConversao(orcamento.id, contaIdOriginal);
        }
    } else {
        this.contaSelecionadaParaConversao = null;
        new bootstrap.Modal(document.getElementById('modalEscolherConta')).show();
    }
  }

  executarConversao(orcamentoId: number, contaId: number) {
    this.orcamentoService.converterEmVenda(orcamentoId, contaId).subscribe({
        next: () => {
            alert('Sucesso! Vendas geradas, stock abatido e saldo atualizado.');
            
            // Avisa a Tesouraria!
            this.tesourariaService.notificarNovaTransacao(); 
            
            const modalConta = bootstrap.Modal.getInstance(document.getElementById('modalEscolherConta'));
            if(modalConta) modalConta.hide();
        },
        error: (e) => alert('Erro: ' + (e.error?.message || 'Falha ao converter.'))
    });
  }

  confirmarConversaoComNovaConta() {
    if (!this.contaSelecionadaParaConversao) {
        alert('É OBRIGATÓRIO escolher uma conta bancária para faturar!');
        return;
    }
    this.executarConversao(this.orcamentoParaConverter.id, this.contaSelecionadaParaConversao);
  }

  fecharModal() {
    bootstrap.Modal.getInstance(document.getElementById('modalOrcamento'))?.hide();
  }

  getDataDaquiA30Dias(): string {
    const data = new Date();
    data.setDate(data.getDate() + 30);
    return data.toISOString().split('T')[0];
  }
}