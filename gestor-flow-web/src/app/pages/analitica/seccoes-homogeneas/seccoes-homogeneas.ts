import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AnaliticaService } from '../../../services/analitica.service';
import { CentroCusto, SeccaoHomo } from '../../../core/models/analitica.model';

declare var bootstrap: any;

@Component({
  selector: 'app-seccoes-homogeneas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './seccoes-homogeneas.html'
})
export class SeccoesHomoComponent implements OnInit {

  listaSeccoes: SeccaoHomo[] = [];
  listaCentros: CentroCusto[] = []; 
  
  formSeccao!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDados();
  }

  inicializarFormulario() {
    this.formSeccao = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigo: ['', [Validators.required]],
      centroCustoId: [null, [Validators.required]] // Obrigatório escolher o Pai
    });
  }

  get f() { return this.formSeccao.controls; }

  carregarDados() {
    // 1. Carregar a lista principal (Secções)
    this.analiticaService.listarSeccoes().subscribe({
      next: (dados) => {
        this.listaSeccoes = dados;
        this.cd.detectChanges();
      },
      error: (e) => console.error(e)
    });

    // 2. Carregar os Centros para o Dropdown (Pai)
    this.analiticaService.listarCentros().subscribe({
      next: (dados) => this.listaCentros = dados
    });
  }

  // --- MODAL & AÇÕES ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formSeccao.reset();
    
    // Define o primeiro centro como padrão para facilitar
    if (this.listaCentros.length > 0) {
      this.formSeccao.patchValue({ centroCustoId: this.listaCentros[0].id });
    }

    const modal = new bootstrap.Modal(document.getElementById('modalSeccao'));
    modal.show();
  }

  editarSeccao(seccao: SeccaoHomo) {
    this.idEmEdicao = seccao.id!;
    this.formSeccao.patchValue({
      nome: seccao.nome,
      codigo: seccao.codigo,
      // O backend envia o objeto 'centroCusto' completo, extraímos o ID
      centroCustoId: seccao.centroCustoId
    });
    const modal = new bootstrap.Modal(document.getElementById('modalSeccao'));
    modal.show();
  }

  guardarSeccao() {
    if (this.formSeccao.invalid) {
      this.formSeccao.markAllAsTouched();
      return;
    }

    const dto = this.formSeccao.value;

    if (this.idEmEdicao) {
      this.analiticaService.atualizarSeccao(this.idEmEdicao, dto).subscribe({
        next: () => this.finalizar('Secção atualizada!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    } else {
      this.analiticaService.criarSeccao(dto).subscribe({
        next: () => this.finalizar('Secção criada com sucesso!'),
        error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
      });
    }
  }

  eliminarSeccao(id: number) {
    if (confirm('Tem a certeza que quer eliminar esta secção?')) {
      this.analiticaService.eliminarSeccao(id).subscribe({
        next: () => {
          alert('Eliminada com sucesso!');
          this.carregarDados();
        },
        error: (e: any) => alert('Erro ao eliminar.')
      });
    }
  }

  finalizar(msg: string) {
    alert(msg);
    this.carregarDados();
    const modalElement = document.getElementById('modalSeccao');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}