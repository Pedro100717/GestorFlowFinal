import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FornecedorService } from '../../services/fornecedor.service';
import { Fornecedor } from '../../core/models/fornecedor.model';

declare var bootstrap: any;

@Component({
  selector: 'app-fornecedores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './fornecedores.html'
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
    this.carregarFornecedores();
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

  carregarFornecedores() {
    this.fornecedorService.listar().subscribe({
      next: (dados: any) => {
        // Suporte para lista simples ou paginada, por segurança
        this.listaFornecedores = Array.isArray(dados) ? dados : (dados.content || []);
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro:', e)
    });
  }

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
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    } else {
      this.fornecedorService.criar(dados).subscribe({
        next: () => this.finalizar('Fornecedor criado!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    }
  }

  eliminarFornecedor(id: number) {
    if (confirm('Tem a certeza?')) {
      this.fornecedorService.apagar(id).subscribe({
        next: () => {
          alert('Eliminado com sucesso!');
          this.carregarFornecedores();
        },
        error: (e: any) => alert('Erro: Este fornecedor já tem compras associadas.')
      });
    }
  }

  finalizar(msg: string) {
    alert(msg);
    this.carregarFornecedores();
    const modalElement = document.getElementById('modalFornecedor');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}