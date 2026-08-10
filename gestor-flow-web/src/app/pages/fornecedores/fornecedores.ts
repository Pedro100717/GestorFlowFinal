import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 IMPORT DOS ERROS HTTP
import { FornecedorService } from '../../services/fornecedor.service';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { LogService } from '../../core/services/log.service'; // 🚀 INJEÇÃO DO INSPETOR

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
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 SERVIÇO DECLARADO NO CONSTRUTOR
  ) {}

  ngOnInit() {
    this.inicializarFormulario();

    // O ecrã fica à escuta do cofre. Se a memória mudar, a tabela atualiza na hora!
    this.fornecedorService.fornecedores$.subscribe(fornecedores => {
      this.listaFornecedores = fornecedores;
      this.cd.detectChanges();
    });

    // Manda o serviço encher o cofre pela primeira vez
    this.fornecedorService.carregarFornecedoresDaAPI();
  }

  inicializarFormulario() {
    this.formFornecedor = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.pattern('^[0-9]{9}$')]], 
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      website: [''] 
    });
  }

  get f() { return this.formFornecedor.controls; }

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
        next: () => {
          this.logService.debug(`Fornecedor ${this.idEmEdicao} atualizado com sucesso.`); // 🚀 RASTREABILIDADE
          this.finalizar('Fornecedor atualizado!');
        },
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao atualizar fornecedor', e); // 🚀 CAIXA NEGRA
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
        next: () => {
          this.logService.debug('Novo fornecedor criado com sucesso.'); // 🚀 RASTREABILIDADE
          this.finalizar('Fornecedor criado!');
        },
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao criar fornecedor', e); // 🚀 CAIXA NEGRA
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
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este fornecedor será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.fornecedorService.apagar(id).subscribe({
          next: () => {
            this.logService.info(`Fornecedor ${id} eliminado com sucesso.`); // 🚀 RASTREABILIDADE
            Swal.fire('Eliminado!', 'O fornecedor foi apagado com sucesso.', 'success');
          },
          error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
            this.logService.error(`Falha ao eliminar fornecedor ${id}`, e); // 🚀 CAIXA NEGRA
            Swal.fire('Erro!', 'Este fornecedor não pode ser apagado porque já tem compras associadas.', 'error');
          }
        });
      }
    });
  }

  finalizar(msg: string) {
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