import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 1. IMPORT OBRIGATÓRIO DOS ERROS HTTP
import { AnaliticaService } from '../../../services/analitica.service';
import { CentroCusto } from '../../../core/models/analitica.model';
import { LogService } from '../../../core/services/log.service';
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-centros-custo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './centros-custo.html',
  styleUrl: './centros-custo.scss'
})
export class CentrosCustoComponent implements OnInit {

  listaCentros: CentroCusto[] = [];
  formCentro!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarCentros();
  }

  inicializarFormulario() {
    this.formCentro = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigo: ['', [Validators.required]] 
    });
  }

  get f() { return this.formCentro.controls; }

  carregarCentros() {
    this.analiticaService.listarCentros().subscribe({
      next: (dados) => {
        this.listaCentros = dados;
        this.cd.detectChanges();
        this.logService.debug('Centros de Custo carregados com sucesso.');
      },
      // 🚀 2. TIPAGEM ESTRITA
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar centros:', e)
    });
  }

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formCentro.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalCentro'));
    modal.show();
  }

  editarCentro(centro: CentroCusto) {
    this.idEmEdicao = centro.id!;
    this.formCentro.patchValue({
      nome: centro.nome,
      codigo: centro.codigo
    });
    const modal = new bootstrap.Modal(document.getElementById('modalCentro'));
    modal.show();
  }

  guardarCentro() {
    if (this.formCentro.invalid) {
      this.formCentro.markAllAsTouched();
      return;
    }

    const dto = this.formCentro.value;

    if (this.idEmEdicao) {
      this.analiticaService.atualizarCentro(this.idEmEdicao, dto).subscribe({
        next: () => this.finalizar('Centro de Custo atualizado!'),
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao atualizar centro de custo', e); // 🚀 PRIMEIRO REGISTAMOS NA CAIXA NEGRA
          Swal.fire({                                                     // 🚀 DEPOIS AVISAMOS O UTILIZADOR
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível atualizar o centro de custo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.analiticaService.criarCentro(dto).subscribe({
        next: () => this.finalizar('Centro de Custo criado!'),
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao criar centro de custo', e); // 🚀 CAIXA NEGRA PRIMEIRO
          Swal.fire({                                                 // 🚀 UX DEPOIS
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar o centro de custo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarCentro(id: number) {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Se este centro tiver secções ou movimentos, não será apagado.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.analiticaService.eliminarCentro(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O Centro de Custo foi apagado.', 'success');
            this.carregarCentros();
            this.logService.info(`Centro de custo ${id} eliminado com sucesso.`); // 🚀 RASTREABILIDADE
          },
          error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
            this.logService.error(`Falha ao eliminar centro de custo ${id}`, e); // 🚀 CAIXA NEGRA
            Swal.fire('Erro!', e.error?.message || 'Provavelmente este centro já está em uso noutros locais.', 'error'); // 🚀 UX
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

    this.carregarCentros();
    const modalElement = document.getElementById('modalCentro');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}