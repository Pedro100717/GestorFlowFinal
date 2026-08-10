import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators, FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 1. IMPORT OBRIGATÓRIO DOS ERROS
import { OrcamentoService } from '../../services/orcamento.service';
import { ClienteService } from '../../services/cliente.service';
import { ArtigoService } from '../../services/artigo.service';
import { VendaService } from '../../services/venda.service'; 
import { TesourariaService } from '../../services/tesouraria.service'; 
import { Orcamento } from '../../core/models/orcamento.model';
import { LogService } from '../../core/services/log.service';
import { IvaService } from '../../services/iva.service';

// IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-orcamentos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], 
  templateUrl: './orcamentos.html',
  styleUrls: ['./orcamentos.scss']
})
export class OrcamentosComponent implements OnInit {

  listaOrcamentos: Orcamento[] = [];
  listaClientes: any[] = []; // ⚠️ Dica: Substituir 'any' por 'Cliente'
  listaArtigos: any[] = [];  // ⚠️ Dica: Substituir 'any' por 'Artigo'
  listaTaxasIva: any[] = []; // ⚠️ Dica: Substituir 'any' por 'TaxaIva'
  listaContas: any[] = [];   // ⚠️ Dica: Substituir 'any' por 'ContaBancaria'
  
  formOrcamento!: FormGroup;
  idEmEdicao: number | null = null;
  totalGeralPrevisto: number = 0;

  orcamentoParaConverter: any = null; // ⚠️ Dica: Substituir por 'Orcamento'
  contaSelecionadaParaConversao: number | null = null;

  constructor(
    private orcamentoService: OrcamentoService,
    private clienteService: ClienteService,
    private artigoService: ArtigoService,
    private ivaService: IvaService,
    private tesourariaService: TesourariaService, 
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 3. SERVIÇO DECLARADO
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDadosIniciais();

    this.orcamentoService.orcamentos$.subscribe(lista => {
      this.listaOrcamentos = lista;
      this.cd.detectChanges();
    });

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
    this.tesourariaService.carregarContasDaAPI(); 

    this.clienteService.listar().subscribe({
      next: (d: any) => this.listaClientes = d.content || d, // 🚀 Tipagem resolvida
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar clientes no Orçamento', e)
    });
    
    this.artigoService.listar().subscribe({
      next: (d: any) => this.listaArtigos = d.content || d, // 🚀 Tipagem resolvida
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar artigos no Orçamento', e)
    });

    // 🚀 BATE NO SERVIÇO CORRETO E COM A TIPAGEM RESOLVIDA
    this.ivaService.listar().subscribe({
      next: (d: any[]) => this.listaTaxasIva = d, 
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar IVAs no Orçamento', e)
    });
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
      Swal.fire({
        icon: 'warning',
        title: 'Atenção',
        text: 'Por favor, preenche todos os campos obrigatórios antes de guardar.',
        confirmButtonColor: '#0d6efd'
      });
      return;
    }

    const dto = this.formOrcamento.value;

    const request$ = this.idEmEdicao 
      ? this.orcamentoService.atualizar(this.idEmEdicao, dto)
      : this.orcamentoService.criar(dto);

    request$.subscribe({
      next: () => {
        this.logService.info(this.idEmEdicao ? `Orçamento ${this.idEmEdicao} atualizado.` : 'Novo orçamento guardado.'); // 🚀 RASTREABILIDADE
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: this.idEmEdicao ? 'Orçamento atualizado!' : 'Orçamento guardado!' });
        this.fecharModal();
      },
      error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error('Erro ao guardar o orçamento', e); // 🚀 CAIXA NEGRA
        Swal.fire({
          icon: 'error',
          title: 'Erro ao guardar',
          text: e.error?.message || 'Ocorreu um erro ao processar o orçamento.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }

  prepararConversao(orcamento: any) {
    this.orcamentoParaConverter = orcamento;
    const contaIdOriginal = orcamento.contaBancariaId || orcamento.contaBancaria?.id;

    if (contaIdOriginal) {
        Swal.fire({
          title: 'Faturar Orçamento?',
          text: "Este orçamento já tem uma conta associada. Pretende faturar e abater stock agora?",
          icon: 'question',
          showCancelButton: true,
          confirmButtonColor: '#198754', 
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sim, faturar!',
          cancelButtonText: 'Cancelar'
        }).then((result) => {
          if (result.isConfirmed) {
            this.executarConversao(orcamento.id, contaIdOriginal);
          }
        });
    } else {
        this.contaSelecionadaParaConversao = null;
        new bootstrap.Modal(document.getElementById('modalEscolherConta')).show();
    }
  }

  executarConversao(orcamentoId: number, contaId: number) {
    this.orcamentoService.converterEmVenda(orcamentoId, contaId).subscribe({
        next: () => {
            this.logService.info(`Orçamento ${orcamentoId} convertido em fatura/venda na conta ${contaId}.`); // 🚀 RASTREABILIDADE
            Swal.fire('Faturado!', 'As vendas foram geradas, o stock abatido e o saldo atualizado.', 'success');
            
            this.tesourariaService.notificarNovaTransacao(); 
            
            const modalConta = bootstrap.Modal.getInstance(document.getElementById('modalEscolherConta'));
            if(modalConta) modalConta.hide();
        },
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error(`Falha ao converter orçamento ${orcamentoId}`, e); // 🚀 CAIXA NEGRA
          Swal.fire('Erro na Conversão', e.error?.message || 'Falha ao converter o orçamento.', 'error');
        }
    });
  }

  confirmarConversaoComNovaConta() {
    if (!this.contaSelecionadaParaConversao) {
        Swal.fire('Atenção', 'É OBRIGATÓRIO escolher uma conta bancária para faturar!', 'warning');
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

  visualizarPdf(id: number | undefined) {
    if (!id) {
      Swal.fire('Atenção', 'Este orçamento ainda não tem um ID válido.', 'warning');
      return;
    }

    this.orcamentoService.abrirPdfOrcamento(id).subscribe({
      next: (arquivoBlob: Blob) => {
        this.logService.debug(`PDF gerado para orçamento ${id}.`); // 🚀 RASTREABILIDADE
        const fileURL = URL.createObjectURL(arquivoBlob);
        window.open(fileURL, '_blank');
      },
      error: (erro: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA E ADEUS CONSOLE.ERROR
        this.logService.error(`Falha na geração do PDF para o orçamento ${id}`, erro); // 🚀 CAIXA NEGRA
        Swal.fire({
          icon: 'error',
          title: 'Erro na Geração',
          text: 'Não foi possível gerar o PDF deste Orçamento. Tenta novamente.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}