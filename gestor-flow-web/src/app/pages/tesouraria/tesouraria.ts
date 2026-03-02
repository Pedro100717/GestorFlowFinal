import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TesourariaService } from '../../services/tesouraria.service';
import { ContaBancaria, Movimento } from '../../core/models/tesouraria.model'; 

declare var bootstrap: any;

@Component({
  selector: 'app-tesouraria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tesouraria.html'
})
export class TesourariaComponent implements OnInit {

  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;

  formConta!: FormGroup;
  formMovimento!: FormGroup;
  formTransferencia!: FormGroup;

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

    // Arrancar a passadeira rolante!
    this.tesourariaService.carregarContasDaAPI();
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
  }

  novaConta() {
    this.formConta.reset({ saldoInicial: 0, iban: 'PT50' }); 
    new bootstrap.Modal(document.getElementById('modalConta')).show();
  }

  guardarConta() {
    if (this.formConta.invalid) {
      this.formConta.markAllAsTouched();
      return;
    }
    this.tesourariaService.criarConta(this.formConta.value).subscribe({
      next: () => {
        alert('Conta criada com sucesso!');
        bootstrap.Modal.getInstance(document.getElementById('modalConta'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || 'Verifica os dados.'))
    });
  }

  novoMovimento() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formMovimento.reset({ 
      contaId: contaId, 
      tipo: 'DEBITO', 
      valor: 0,
      descricao: ''
    });
    new bootstrap.Modal(document.getElementById('modalMovimento')).show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) {
      this.formMovimento.markAllAsTouched();
      return;
    }
    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe({
      next: () => {
        alert('Movimento registado!');
        bootstrap.Modal.getInstance(document.getElementById('modalMovimento'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }

  verExtrato(conta: ContaBancaria) {
    this.contaSelecionada = conta;
    // Basta dizer ao serviço de qual conta queremos os movimentos. O Cofre trata do resto.
    this.tesourariaService.obterExtrato(conta.id!);
  }

  novaTransferencia() {
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    this.formTransferencia.reset({
      contaOrigemId: contaId,
      contaDestinoId: null,
      valor: 0,
      descricao: ''
    });
    new bootstrap.Modal(document.getElementById('modalTransferencia')).show();
  }

  realizarTransferencia() {
    if (this.formTransferencia.invalid) {
      this.formTransferencia.markAllAsTouched();
      return;
    }

    const origem = this.formTransferencia.get('contaOrigemId')?.value;
    const destino = this.formTransferencia.get('contaDestinoId')?.value;
    
    if (origem === destino) {
      alert('A conta de origem e destino não podem ser a mesma.');
      return;
    }

    this.tesourariaService.realizarTransferencia(this.formTransferencia.value).subscribe({
      next: () => {
        alert('Transferência realizada com sucesso!');
        bootstrap.Modal.getInstance(document.getElementById('modalTransferencia'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }
}