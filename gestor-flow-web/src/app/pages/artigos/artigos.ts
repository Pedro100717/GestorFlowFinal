import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ArtigoService } from '../../services/artigo.service';
import { Artigo } from '../../core/models/artigo.model';

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
    this.carregarArtigos();
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

  carregarArtigos() {
    this.artigoService.listar().subscribe({
      next: (dados: any) => {
        this.listaArtigos = dados.content || dados;
        this.cd.detectChanges();
      },
      error: (erro) => console.error('Erro ao carregar artigos:', erro)
    });
  }

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
      movimentaStock: artigo.movimentaStock,
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
        error: (e) => alert('Erro: ' + (e.error?.message || e.message))
      });
    } else {
      this.artigoService.criar(dados).subscribe({
        next: () => this.finalizarAcao('Artigo criado! O stock começa a 0.'),
        error: (e) => alert('Erro: ' + (e.error?.message || e.message))
      });
    }
  }

  eliminarArtigo(id: number) {
    if (confirm('Tens a certeza que queres eliminar este artigo?')) {
      this.artigoService.apagar(id).subscribe({
        next: () => {
          alert('Artigo eliminado!');
          this.carregarArtigos();
        },
        error: (e) => alert('Erro ao eliminar.')
      });
    }
  }

  finalizarAcao(msg: string) {
    alert(msg);
    this.carregarArtigos();
    const modalElement = document.getElementById('modalArtigo');
    if (modalElement) {
       const modal = bootstrap.Modal.getInstance(modalElement);
       modal?.hide();
    }
  }

  abrirModal() {
    const el = document.getElementById('modalArtigo');
    if(el) {
        // Garante que abre limpo
        let modal = bootstrap.Modal.getInstance(el);
        if (!modal) modal = new bootstrap.Modal(el);
        modal.show();
    }
  }
}