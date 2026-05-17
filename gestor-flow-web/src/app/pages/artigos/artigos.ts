import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ArtigoService } from '../../services/artigo.service';
import { Artigo } from '../../core/models/artigo.model';
import Swal from 'sweetalert2';
import * as bootstrap from 'bootstrap';

@Component({
  selector: 'app-artigos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './artigos.html',
  styleUrl: './artigos.scss'
})
export class ArtigosComponent implements OnInit {

  listaArtigos: Artigo[] = [];
  formArtigo!: FormGroup;
  idEmEdicao: number | null = null;
  
  // 🚀 A TRANCA DA PORTA (Impede Duplos Cliques)
  isGuardando: boolean = false; 

  constructor(
    private artigoService: ArtigoService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    
    this.artigoService.artigos$.subscribe(artigos => {
      this.listaArtigos = artigos;
      this.cd.detectChanges();
    });

    this.artigoService.carregarArtigosDaAPI();
  }

  inicializarFormulario() {
    this.formArtigo = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigoBarras: [''],
      movimentaStock: [true],
    });
  }

  get f() { return this.formArtigo.controls; }

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formArtigo.reset({
      nome: '',
      codigoBarras: '',
      movimentaStock: true, 
    });
    // 🚀 Destranca o switch para novos artigos
    this.formArtigo.get('movimentaStock')?.enable();
    this.abrirModal();
  }

  editarArtigo(artigo: Artigo) {
    this.idEmEdicao = artigo.id!; 
  
    this.formArtigo.patchValue({
      nome: artigo.nome,
      codigoBarras: artigo.codigoBarras,
      movimentaStock: artigo.tipo === 'MERCADORIA',
      familiaId: artigo.familiaId
    });

    // 🚀 Tranca o switch para impedir alterações à natureza do artigo!
    this.formArtigo.get('movimentaStock')?.disable();
  
    this.abrirModal();
  }

  guardarArtigo() {
    // 🚀 SE JÁ ESTIVER A GUARDAR, IGNORA OS CLIQUES EXTRA
    if (this.formArtigo.invalid || this.isGuardando) {
      this.formArtigo.markAllAsTouched();
      return;
    }
    
    this.isGuardando = true; // 🚀 FECHA A PORTA
    
    // 🚀 CRÍTICO: Usar getRawValue() para apanhar os dados dos campos que estão disabled (trancados)
    const dados = this.formArtigo.getRawValue();

    if (this.idEmEdicao) {
      this.artigoService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => this.finalizarAcao('Artigo atualizado!'),
        error: (e) => {
          this.isGuardando = false; // 🚀 ABRE A PORTA DE NOVO
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível atualizar o artigo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.artigoService.criar(dados).subscribe({
        next: () => this.finalizarAcao('Artigo criado! O stock começa a 0.'),
        error: (e) => {
          this.isGuardando = false; // 🚀 ABRE A PORTA DE NOVO
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar o artigo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarArtigo(id: number) {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este artigo será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.artigoService.apagar(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O artigo foi apagado.', 'success');
          },
          error: (e) => {
            Swal.fire('Erro!', 'Não foi possível apagar. Pode estar associado a orçamentos, compras ou vendas.', 'error');
          }
        });
      }
    });
  }

  finalizarAcao(msg: string) {
    this.isGuardando = false; // 🚀 ABRE A PORTA PARA A PRÓXIMA AÇÃO

    const Toast = Swal.mixin({
      toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
    });
    Toast.fire({ icon: 'success', title: msg });

    const modalElement = document.getElementById('modalArtigo');
    if (modalElement) {
       const modal = bootstrap.Modal.getInstance(modalElement);
       modal?.hide();
    }
  }

  abrirModal() {
    const el = document.getElementById('modalArtigo');
    if(el) {
        let modal = bootstrap.Modal.getInstance(el);
        if (!modal) modal = new bootstrap.Modal(el);
        modal.show();
    }
  }
}