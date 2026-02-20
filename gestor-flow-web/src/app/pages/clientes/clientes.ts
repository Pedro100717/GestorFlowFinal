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
    this.carregarClientes();
  }

  inicializarFormulario() {
    this.formCliente = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.pattern('^[0-9]{9}$')]], // Validação visual simples (9 dígitos)
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      anotacoes: ['']
    });
  }

  get f() { return this.formCliente.controls; }

  carregarClientes() {
    this.clienteService.listar().subscribe({
      next: (dados) => {
        // Suporte para Paginação (Spring retorna objeto Page) ou Lista direta
        this.listaClientes = dados.content || dados;
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro ao carregar clientes:', e)
    });
  }

  // --- AÇÕES ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formCliente.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalCliente'));
    modal.show();
  }

  editarCliente(cliente: Cliente) {
    this.idEmEdicao = cliente.id!;
    this.formCliente.patchValue(cliente); // Preenche tudo automaticamente
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
        next: () => {
          alert('Cliente eliminado!');
          this.carregarClientes();
        },
        error: (e: any) => alert('Erro: Este cliente pode ter vendas associadas.')
      });
    }
  }

  finalizar(msg: string) {
    alert(msg);
    this.carregarClientes();
    const modal = bootstrap.Modal.getInstance(document.getElementById('modalCliente'));
    modal?.hide();
  }
}