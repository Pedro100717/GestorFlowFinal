import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ClienteService } from '../../services/cliente.service';
import { Cliente } from '../../core/models/cliente.model';

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

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
        // 2. ERRO ELEGANTE AQUI
        error: (e: any) => {
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Verifica o NIF ou os dados inseridos.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.clienteService.criar(dados).subscribe({
        next: () => this.finalizar('Cliente criado com sucesso!'),
        // 2. ERRO ELEGANTE AQUI
        error: (e: any) => {
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar. NIF duplicado ou inválido?',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarCliente(id: number) {
    // 3. O FIM DO "confirm()" FEIO
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Isto apagará o histórico deste cliente permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      
      if (result.isConfirmed) {
        this.clienteService.apagar(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O cliente foi apagado com sucesso.', 'success');
          },
          error: (e: any) => {
            Swal.fire('Erro!', 'Este cliente não pode ser apagado porque já tem vendas associadas.', 'error');
          }
        });
      }
    });
  }

  finalizar(msg: string) {
    // 4. TOAST DE SUCESSO NO CANTO SUPERIOR
    const Toast = Swal.mixin({
      toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
    });
    Toast.fire({ icon: 'success', title: msg });

    const modal = bootstrap.Modal.getInstance(document.getElementById('modalCliente'));
    modal?.hide();
  }
}