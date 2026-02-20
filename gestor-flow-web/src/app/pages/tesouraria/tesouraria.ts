import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TesourariaService } from '../../services/tesouraria.service';
// Importa os modelos para segurança de tipos
import { ContaBancaria, Movimento } from '../../core/models/tesouraria.model'; 

declare var bootstrap: any;

@Component({
  selector: 'app-tesouraria',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tesouraria.html'
})
export class TesourariaComponent implements OnInit {

  // Listas tipadas corretamente
  listaContas: ContaBancaria[] = [];
  movimentos: Movimento[] = [];
  contaSelecionada: ContaBancaria | null = null;

  formConta!: FormGroup;
  formMovimento!: FormGroup;

  constructor(
    private tesourariaService: TesourariaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // 1. Inicializar Formulário de Conta
    this.formConta = this.fb.group({
      nome: ['', Validators.required],
      iban: [''], 
      saldoInicial: [0]
    });

    // 2. Inicializar Formulário de Movimento
    this.formMovimento = this.fb.group({
      contaId: [null, Validators.required],
      descricao: ['', Validators.required],
      tipo: ['DEBITO', Validators.required], // DEBITO é o padrão
      valor: [0, [Validators.required, Validators.min(0.01)]]
    });

    // 3. Carregar dados
    this.carregarContas();
  }

  carregarContas() {
    this.tesourariaService.listarContas().subscribe({
      next: (dados: any[]) => {
        // O backend devolve a lista de contas
        this.listaContas = dados;
        
        // Se já tínhamos uma conta aberta, atualizamos os dados dela (ex: saldo novo)
        if (this.contaSelecionada) {
          const contaAtualizada = this.listaContas.find(c => c.id === this.contaSelecionada!.id);
          if (contaAtualizada) {
            this.contaSelecionada = contaAtualizada;
            // Opcional: recarregar o extrato também para ver o movimento novo
            this.verExtrato(this.contaSelecionada); 
          }
        }
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro ao carregar contas:', e)
    });
  }

  // --- Lógica de Contas ---

  novaConta() {
    this.formConta.reset({ saldoInicial: 0, iban: 'PT50' }); 
    const modal = new bootstrap.Modal(document.getElementById('modalConta'));
    modal.show();
  }

  guardarConta() {
    if (this.formConta.invalid) {
      this.formConta.markAllAsTouched();
      return;
    }
    
    this.tesourariaService.criarConta(this.formConta.value).subscribe({
      next: () => {
        alert('Conta criada com sucesso!');
        this.carregarContas();
        // Fechar modal
        const modalEl = document.getElementById('modalConta');
        if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
      },
      error: (e) => alert('Erro: ' + (e.error?.message || 'Verifica os dados.'))
    });
  }

  // --- Lógica de Movimentos ---

  novoMovimento() {
    // Se uma conta estiver selecionada, pré-seleciona no dropdown
    const contaId = this.contaSelecionada ? this.contaSelecionada.id : null;
    
    this.formMovimento.reset({ 
      contaId: contaId, 
      tipo: 'DEBITO', 
      valor: 0,
      descricao: ''
    });

    const modal = new bootstrap.Modal(document.getElementById('modalMovimento'));
    modal.show();
  }

  registarMovimento() {
    if (this.formMovimento.invalid) {
      this.formMovimento.markAllAsTouched();
      return;
    }

    this.tesourariaService.registarMovimento(this.formMovimento.value).subscribe({
      next: () => {
        alert('Movimento registado!');
        this.carregarContas(); // Atualiza saldos
        
        const modalEl = document.getElementById('modalMovimento');
        if (modalEl) bootstrap.Modal.getInstance(modalEl)?.hide();
      },
      error: (e) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }

  verExtrato(conta: ContaBancaria) {
    this.contaSelecionada = conta;
    // Limpa movimentos antigos enquanto carrega os novos
    this.movimentos = []; 
    
    this.tesourariaService.obterExtrato(conta.id!).subscribe({
      next: (dados: any[]) => {
        this.movimentos = dados;
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro ao obter extrato:', e)
    });
  }
}