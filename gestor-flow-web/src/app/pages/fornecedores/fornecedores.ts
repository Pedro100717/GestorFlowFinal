import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FornecedorService } from '../../services/fornecedor.service';
import { Fornecedor } from '../../core/models/fornecedor.model';

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-fornecedores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './fornecedores.html',
  styleUrl: './fornecedores.scss'
})
export class FornecedoresComponent implements OnInit {

  listaFornecedores: Fornecedor[] = [];
  formFornecedor!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();

    // --- A MÁGICA REATIVA ---
    // 1. O ecrã fica à escuta do cofre. Se a memória mudar, a tabela atualiza na hora!
    this.fornecedorService.fornecedores$.subscribe(fornecedores => {
      this.listaFornecedores = fornecedores;
      this.cd.detectChanges();
    });

    // 2. Manda o serviço encher o cofre pela primeira vez
    this.fornecedorService.carregarFornecedoresDaAPI();
  }

  inicializarFormulario() {
    this.formFornecedor = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.pattern('^[0-9]{9}$')]], 
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      website: [''] // Campo novo
    });
  }

  get f() { return this.formFornecedor.controls; }

  // --- AÇÕES ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formFornecedor.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalFornecedor'));
    modal.show();
  }

  editarFornecedor(f: Fornecedor) {
    this.idEmEdicao = f.id!;
    this.formFornecedor.patchValue(f);
    const modal = new bootstrap.Modal(document.getElementById('modalFornecedor'));
    modal.show();
  }

  guardarFornecedor() {
    if (this.formFornecedor.invalid) {
      this.formFornecedor.markAllAsTouched();
      return;
    }

    const dados = this.formFornecedor.value;

    if (this.idEmEdicao) {
      this.fornecedorService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => this.finalizar('Fornecedor atualizado!'),
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
      this.fornecedorService.criar(dados).subscribe({
        next: () => this.finalizar('Fornecedor criado!'),
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

  eliminarFornecedor(id: number) {
    // 3. O FIM DO "confirm()" FEIO
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este fornecedor será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      
      if (result.isConfirmed) {
        this.fornecedorService.apagar(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O fornecedor foi apagado com sucesso.', 'success');
          },
          error: (e: any) => {
            Swal.fire('Erro!', 'Este fornecedor não pode ser apagado porque já tem compras associadas.', 'error');
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

    const modalElement = document.getElementById('modalFornecedor');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}