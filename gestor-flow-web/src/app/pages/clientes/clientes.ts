import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteService } from '../../services/cliente.service';
import { Cliente } from '../../core/models/cliente.model';

declare var bootstrap: any;

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './clientes.html'
})
export class ClientesComponent implements OnInit {

  listaClientes: Cliente[] = [];
  formCliente!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private clienteService: ClienteService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();

    // --- A MÁGICA REATIVA ---
    // 1. O ecrã fica à escuta do cofre. Se a memória mudar, a tabela atualiza na hora!
    this.clienteService.clientes$.subscribe(clientes => {
      this.listaClientes = clientes;
      this.cd.detectChanges();
    });

    // 2. Manda o serviço encher o cofre pela primeira vez
    this.clienteService.carregarClientesDaAPI();
  }

  inicializarFormulario() {
    this.formCliente = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.pattern('^[0-9]{9}$')]], 
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      anotacoes: ['']
    });
  }

  get f() { return this.formCliente.controls; }

  // --- AÇÕES ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formCliente.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalCliente'));
    modal.show();
  }

  editarCliente(cliente: Cliente) {
    this.idEmEdicao = cliente.id!;
    this.formCliente.patchValue(cliente); 
    const modal = new bootstrap.Modal(document.getElementById('modalCliente'));
    modal.show();
  }

  guardarCliente() {
    if (this.formCliente.invalid) {
      this.formCliente.markAllAsTouched();
      return;
    }

    const dados = this.formCliente.value;

    if (this.idEmEdicao) {
      this.clienteService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => this.finalizar('Cliente atualizado!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || 'Verifica o NIF ou dados.'))
      });
    } else {
      this.clienteService.criar(dados).subscribe({
        next: () => this.finalizar('Cliente criado com sucesso!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || 'NIF duplicado ou inválido.'))
      });
    }
  }

  eliminarCliente(id: number) {
    if (confirm('Tem a certeza? Isto apagará o histórico deste cliente.')) {
      this.clienteService.apagar(id).subscribe({
        // Sem delay! Apenas dá o alerta de sucesso.
        next: () => alert('Cliente eliminado!'),
        error: (e: any) => alert('Erro: Este cliente pode ter vendas associadas.')
      });
    }
  }

  finalizar(msg: string) {
    alert(msg);
    // REMOVIDO: this.carregarClientes()! A tabela atualiza sozinha pelo Cofre.
    const modal = bootstrap.Modal.getInstance(document.getElementById('modalCliente'));
    modal?.hide();
  }
}