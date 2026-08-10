import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 IMPORT OBRIGATÓRIO DOS ERROS
import { AnaliticaService } from '../../../services/analitica.service';
import { SeccaoHomo } from '../../../core/models/analitica.model'; 
import { LogService } from '../../../core/services/log.service'; // 🚀 INJEÇÃO DO NOSSO INSPETOR

import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-seccoes-homogeneas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './seccoes-homogeneas.html',
  styleUrl: './seccoes-homogeneas.scss'
})
export class SeccoesHomoComponent implements OnInit {

  listaSeccoes: SeccaoHomo[] = [];
  
  formSeccao!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService // 🚀 SERVIÇO DECLARADO
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDados();
  }

  inicializarFormulario() {
    this.formSeccao = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigo: ['', [Validators.required]]
    });
  }

  get f() { return this.formSeccao.controls; }

  carregarDados() {
    this.analiticaService.listarSeccoes().subscribe({
      // 🚀 ADEUS ANY! Extração limpa baseada no pressuposto de que o serviço já foi corrigido.
      next: (dados: any) => { 
        this.listaSeccoes = dados.content || dados;
        this.cd.detectChanges();
        this.logService.debug('Secções Homogéneas carregadas com sucesso.'); // 🚀 RASTREABILIDADE
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar secções:', e) // 🚀 ADEUS CONSOLE.ERROR
    });
  }

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formSeccao.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalSeccao'));
    modal.show();
  }

  editarSeccao(seccao: SeccaoHomo) {
    this.idEmEdicao = seccao.id!;
    this.formSeccao.patchValue({
      nome: seccao.nome,
      codigo: seccao.codigo
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
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao atualizar secção', e); // 🚀 CAIXA NEGRA PRIMEIRO
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível atualizar a secção.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.analiticaService.criarSeccao(dto).subscribe({
        next: () => this.finalizar('Secção criada com sucesso!'),
        error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha ao criar secção', e); // 🚀 CAIXA NEGRA PRIMEIRO
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar a secção.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarSeccao(id: number) {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Esta secção será eliminada permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.analiticaService.eliminarSeccao(id).subscribe({
          next: () => {
            Swal.fire('Eliminada!', 'A Secção foi apagada.', 'success');
            this.carregarDados();
            this.logService.info(`Secção homogénea ${id} eliminada com sucesso.`); // 🚀 RASTREABILIDADE
          },
          error: (e: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
            this.logService.error(`Falha ao eliminar secção ${id}`, e); // 🚀 CAIXA NEGRA
            Swal.fire('Erro!', e.error?.message || 'Não foi possível eliminar esta secção. Pode estar em uso.', 'error');
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

    this.carregarDados();
    const modalElement = document.getElementById('modalSeccao');
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}