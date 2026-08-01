import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AnaliticaService } from '../../../services/analitica.service';
import { CentroCusto } from '../../../core/models/analitica.model';

// 1. IMPORTAR A NOSSA BIBLIOTECA DE ALERTAS
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
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarCentros();
  }

  inicializarFormulario() {
    this.formCentro = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigo: ['', [Validators.required]] // Ex: "ADM", "PROD"
    });
  }

  get f() { return this.formCentro.controls; }

  carregarCentros() {
    this.analiticaService.listarCentros().subscribe({
      next: (dados) => {
        this.listaCentros = dados;
        this.cd.detectChanges();
      },
      error: (e) => console.error('Erro ao carregar centros:', e)
    });
  }

  // --- MODAL & AÇÕES ---

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
        // 2. ERRO ELEGANTE AQUI
        error: (e: any) => {
          Swal.fire({
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
        // 2. ERRO ELEGANTE AQUI
        error: (e: any) => {
          Swal.fire({
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
    // 3. O FIM DO "confirm()" FEIO
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Se este centro tiver secções ou movimentos, não será apagado.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      
      // Se o utilizador clicou no botão de eliminar
      if (result.isConfirmed) {
        this.analiticaService.eliminarCentro(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O Centro de Custo foi apagado.', 'success');
            this.carregarCentros();
          },
          error: (e: any) => {
            Swal.fire('Erro!', 'Provavelmente este centro já está em uso noutros locais.', 'error');
          }
        });
      }
    });
  }

  finalizar(msg: string) {
    // 4. TOAST PEQUENINO PARA SUCESSO (Para não chatear o utilizador)
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