import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AnaliticaService } from '../../../services/analitica.service';
import { CentroCusto } from '../../../core/models/analitica.model';

declare var bootstrap: any;

@Component({
  selector: 'app-centros-custo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './centros-custo.html'
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
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    } else {
      this.analiticaService.criarCentro(dto).subscribe({
        next: () => this.finalizar('Centro de Custo criado!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    }
  }

  eliminarCentro(id: number) {
    if (confirm('Tem a certeza? Se este centro tiver secções ou movimentos, não será apagado.')) {
      this.analiticaService.eliminarCentro(id).subscribe({
        next: () => {
          alert('Eliminado com sucesso!');
          this.carregarCentros();
        },
        error: (e: any) => alert('Erro: Provavelmente este centro já está em uso.')
      });
    }
  }

  finalizar(msg: string) {
    alert(msg);
    this.carregarCentros();
    const modalElement = document.getElementById('modalCentro');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}