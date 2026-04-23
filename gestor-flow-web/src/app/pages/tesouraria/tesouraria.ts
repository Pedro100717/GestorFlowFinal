import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TesourariaService } from '../../services/tesouraria.service';
import { ContaBancaria, Movimento } from '../../core/models/tesouraria.model'; 

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-tesouraria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tesouraria.html'
})
export class TesourariaComponent implements OnInit {

  abaAtiva: 'contas' | 'pendentes' = 'contas'; // 🛡️ SISTEMA DE ABAS

  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;

  listaPendentes: any[] = []; // 🛡️ NOVA LISTA DE FATURAS POR PAGAR
  docParaConfirmar: any = null; // Guarda o documento para o modal

  formConta!: FormGroup;
  formMovimento!: FormGroup;
  formTransferencia!: FormGroup;
  formConfirmacao!: FormGroup; // 🛡️ FORMULÁRIO DE LIQUIDAÇÃO

  constructor(
    private tesourariaService: TesourariaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormularios();

    // 1. Escutar o Cofre das Contas
    this.tesourariaService.contas$.subscribe(contas => {
      this.listaContas = contas;
      // Garante que o cartão da conta selecionada atualiza o valor no ecrã
      if (this.contaSelecionada) {
        this.contaSelecionada = contas.find(c => c.id === this.contaSelecionada!.id) || null;
      }
      this.cd.detectChanges();
    });

    // 2. Escutar o Cofre do Extrato
    this.tesourariaService.movimentos$.subscribe(movs => {
      this.movimentos = movs;
      this.cd.detectChanges();
    });

    // Arrancar a passadeira rolante e ir buscar faturas ao Java!
    this.carregarDados();
  }

  carregarDados() {
    this.tesourariaService.carregarContasDaAPI();
    this.carregarPendentes();
  }

  carregarPendentes() {
    this.tesourariaService.listarPendentes().subscribe({
      next: (dados) => {
        this.listaPendentes = dados;
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro ao carregar faturas pendentes:', e)
    });
  }

  inicializarFormularios() {
    this.formConta = this.fb.group({
      nome: ['', Validators.required],
      iban: [''], 
      saldoInicial: [0]
    });

    this.formMovimento = this.fb.group({
      contaId: [null, Validators.required],
      descricao: ['', Validators.required],
      tipo: ['DEBITO', Validators.required],
      valor: [0, [Validators.required, Validators.min(0.01)]]
    });

    this.formTransferencia = this.fb.group({
      contaOrigemId: [null, Validators.required],
      contaDestinoId: [null, Validators.required],
      valor: [0, [Validators.required, Validators.min(0.01)]],
      descricao: ['']
    });

    this.formConfirmacao = this.fb.group({
      contaBancariaId: [null, Validators.required],
      dataPagamento: [this.getDataAtual(), Validators.required]
    });
  }

  getDataAtual(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  // ==========================================================
  // --- AÇÕES DO NOVO FLUXO (LIQUIDAR COMPRAS/VENDAS) ---
  // ==========================================================

  abrirModalConfirmacao(doc: any) {
    this.docParaConfirmar = doc;
    
    // Sugere a primeira conta por defeito para poupar tempo
    const contaPadrao = this.listaContas.length > 0 ? this.listaContas[0].id : null;
    
    this.formConfirmacao.reset({ 
      dataPagamento: this.getDataAtual(), 
      contaBancariaId: contaPadrao 
    });
    
    new bootstrap.Modal(document.getElementById('modalConfirmacao')).show();
  }

  confirmarTransacao() {
    if (this.formConfirmacao.invalid) {
      this.formConfirmacao.markAllAsTouched();
      return;
    }

    // Criar o payload exatamente com os nomes que o DTO do Java (ConfirmarPagamentoDTO) espera!
    const payload = {
      documentoId: this.docParaConfirmar.id,
      tipoDocumento: this.docParaConfirmar.tipo,
      contaBancariaId: this.formConfirmacao.value.contaBancariaId,
      dataPagamento: this.formConfirmacao.value.dataPagamento
    };

    this.tesourariaService.confirmarTransacao(payload).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Transação liquidada com sucesso!' });
        
        bootstrap.Modal.getInstance(document.getElementById('modalConfirmacao'))?.hide();
        
        // Vai buscar a lista de pendentes novamente para a fatura desaparecer do ecrã
        this.carregarPendentes();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: 'Falha ao Liquidar',
          text: e.error?.message || 'Ocorreu um erro ao comunicar com a tesouraria.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }

  // ==========================================================
  // --- AÇÕES ANTIGAS (MANUTENÇÃO DE CONTAS E EXTRATOS) ---
  // ==========================================================

  novaConta() {
    this.formConta.reset({ saldoInicial: 0, iban: 'PT50' }); 
    new bootstrap.Modal(document.getElementById('modalConta')).show();
  }

  guardarConta() {
    if (this.formConta.invalid) {
      this.formConta.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Atenção', text: 'Preencha os campos obrigatórios da conta.', confirmButtonColor: '#0d6efd' });
      return;
    }
    
    this.tesourariaService.criarConta(this.formConta.value).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Conta criada com sucesso!' });
        bootstrap.Modal.getInstance(document.getElementById('modalConta'))?.hide();
      },
      error: (e: any) => {
        Swal.fire({ icon: 'error', title: 'Erro ao criar conta', text: e.error?.message || 'Verifica os dados inseridos.', confirmButtonColor: '#0d6efd' });
      }
    });
  }

  novoMovimento() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formMovimento.reset({ contaId: contaId, tipo: 'DEBITO', valor: 0, descricao: '' });
    new bootstrap.Modal(document.getElementById('modalMovimento')).show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) {
      this.formMovimento.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Atenção', text: 'Preencha todos os campos do movimento.', confirmButtonColor: '#0d6efd' });
      return;
    }
    
    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Movimento registado!' });
        bootstrap.Modal.getInstance(document.getElementById('modalMovimento'))?.hide();
      },
      error: (e: any) => {
        Swal.fire({ icon: 'error', title: 'Erro no Movimento', text: e.error?.message || 'Ocorreu um erro ao registar o movimento.', confirmButtonColor: '#0d6efd' });
      }
    });
  }

  verExtrato(conta: ContaBancaria) {
    this.contaSelecionada = conta;
    this.tesourariaService.obterExtrato(conta.id!);
  }

  novaTransferencia() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formTransferencia.reset({ contaOrigemId: contaId, contaDestinoId: null, valor: 0, descricao: '' });
    new bootstrap.Modal(document.getElementById('modalTransferencia')).show();
  }

  realizarTransferencia() {
    if (this.formTransferencia.invalid) {
      this.formTransferencia.markAllAsTouched();
      Swal.fire({ icon: 'warning', title: 'Atenção', text: 'Preencha o valor e selecione as contas.', confirmButtonColor: '#0d6efd' });
      return;
    }

    const origem = this.formTransferencia.get('contaOrigemId')?.value;
    const destino = this.formTransferencia.get('contaDestinoId')?.value;
    
    if (origem === destino) {
      Swal.fire({ icon: 'warning', title: 'Operação Inválida', text: 'A conta de origem e a conta de destino não podem ser a mesma.', confirmButtonColor: '#0d6efd' });
      return;
    }

    this.tesourariaService.realizarTransferencia(this.formTransferencia.value).subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Transferência realizada!' });
        bootstrap.Modal.getInstance(document.getElementById('modalTransferencia'))?.hide();
      },
      error: (e: any) => {
        Swal.fire({ icon: 'error', title: 'Falha na Transferência', text: e.error?.message || 'Ocorreu um erro ao realizar a transferência.', confirmButtonColor: '#0d6efd' });
      }
    });
  }
}