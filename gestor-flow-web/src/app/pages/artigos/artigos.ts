import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ArtigoService } from '../../services/artigo.service';
import { Artigo } from '../../core/models/artigo.model';

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

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

  constructor(
    private artigoService: ArtigoService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    
    // --- A MÁGICA REATIVA ---
    // 1. O ecrã fica à escuta do cofre. Se a memória mudar, a tabela atualiza na hora!
    this.artigoService.artigos$.subscribe(artigos => {
      this.listaArtigos = artigos;
      this.cd.detectChanges();
    });

    // 2. Manda o serviço encher o cofre pela primeira vez
    this.artigoService.carregarArtigosDaAPI();
  }

  inicializarFormulario() {
    this.formArtigo = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigoBarras: [''],
      // Lógica: True = Mercadoria (Stock), False = Serviço
      movimentaStock: [true],
    });
  }

  get f() { return this.formArtigo.controls; }

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formArtigo.reset({
      nome: '',
      codigoBarras: '',
      movimentaStock: true, // Por defeito criamos Mercadorias
    });
    this.abrirModal();
  }

  editarArtigo(artigo: Artigo) {
    this.idEmEdicao = artigo.id!; 
  
    this.formArtigo.patchValue({
      nome: artigo.nome,
      codigoBarras: artigo.codigoBarras,
      // Tradução: Se o tipo for MERCADORIA, o switch "movimentaStock" deve estar ON (true)
      movimentaStock: artigo.tipo === 'MERCADORIA',
      familiaId: artigo.familiaId
    });
  
    this.abrirModal();
  }

  guardarArtigo() {
    if (this.formArtigo.invalid) {
      this.formArtigo.markAllAsTouched();
      return;
    }
    const dados = this.formArtigo.value;

    if (this.idEmEdicao) {
      this.artigoService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => this.finalizarAcao('Artigo atualizado!'),
        // 2. ERRO ELEGANTE AQUI
        error: (e) => {
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
        // 2. ERRO ELEGANTE AQUI
        error: (e) => {
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
    // 3. O FIM DO "confirm()" FEIO
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este artigo será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
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
    // 4. TOAST DE SUCESSO NO CANTO SUPERIOR
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