import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatrimonioService } from '../../services/patrimonio.service';
import { Patrimonio, TipoPatrimonio } from '../../core/models/patrimonio.model';

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-patrimonio',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patrimonio.html',
  styleUrls: ['./patrimonio.scss']
})
export class PatrimonioComponent implements OnInit {

  listaPatrimonio: Patrimonio[] = [];
  formPatrimonio!: FormGroup;
  
  tipoSelecionado: TipoPatrimonio = 'VIATURA';

  constructor(
    private patrimonioService: PatrimonioService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.construirFormulario(); 
    this.carregarDadosIniciais();

    // ESCUTAR O COFRE
    this.patrimonioService.patrimonio$.subscribe((lista: Patrimonio[]) => {
      this.listaPatrimonio = lista;
      this.cd.detectChanges();
    });
  }

  carregarDadosIniciais() {
    this.patrimonioService.carregarPatrimonioDaAPI();
  }

  construirFormulario() {
    const group: any = {
      nome: ['', Validators.required],
      dataAquisicao: [new Date().toISOString().split('T')[0]], 
      valorAquisicao: [0, [Validators.min(0)]]
    };

    if (this.tipoSelecionado === 'VIATURA') {
      group.matricula = ['', Validators.required];
      group.marca = [''];
      group.modelo = [''];
      group.validadeSeguro = [''];
      group.proximaInspecao = [''];
    } 
    else if (this.tipoSelecionado === 'IMOVEL') {
      group.morada = ['', Validators.required];
      group.artigoMatricial = [''];
      group.tipo = ['Urbano']; 
    } 
    else if (this.tipoSelecionado === 'FERRAMENTA') {
      group.numeroSerie = [''];
      group.estadoConservacao = ['Novo'];
    }

    this.formPatrimonio = this.fb.group(group);
  }

  mudarTipo(novoTipo: TipoPatrimonio) {
    this.tipoSelecionado = novoTipo;
    this.construirFormulario();
  }

  abrirModalNovo() {
    this.tipoSelecionado = 'VIATURA'; 
    this.construirFormulario();
    const modal = new bootstrap.Modal(document.getElementById('modalPatrimonio'));
    modal.show();
  }

  guardar() {
    if (this.formPatrimonio.invalid) {
      this.formPatrimonio.markAllAsTouched();
      // 2. AVISO DE FORMULÁRIO INVÁLIDO
      Swal.fire({
        icon: 'warning',
        title: 'Atenção',
        text: 'Por favor, preencha todos os campos obrigatórios.',
        confirmButtonColor: '#0d6efd'
      });
      return;
    }

    const dados = this.formPatrimonio.value;
    let request;

    if (this.tipoSelecionado === 'VIATURA') {
      request = this.patrimonioService.criarViatura(dados);
    } else if (this.tipoSelecionado === 'IMOVEL') {
      request = this.patrimonioService.criarImovel(dados);
    } else {
      request = this.patrimonioService.criarFerramenta(dados);
    }

    request.subscribe({
      next: () => {
        // 3. TOAST DE SUCESSO
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Ativo criado com sucesso!' });
        bootstrap.Modal.getInstance(document.getElementById('modalPatrimonio'))?.hide();
      },
      error: (e: any) => {
        // 4. ERRO ELEGANTE
        Swal.fire({
          icon: 'error',
          title: 'Erro ao guardar',
          text: e.error?.message || 'Ocorreu um erro ao registar o ativo.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }

  eliminar(id: number) {
    // 5. JANELA DE CONFIRMAÇÃO DE ELIMINAÇÃO
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este ativo patrimonial será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.patrimonioService.eliminar(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O ativo foi apagado com sucesso.', 'success');
          },
          error: (e: any) => {
            Swal.fire('Erro!', 'Não foi possível eliminar este ativo.', 'error');
          }
        });
      }
    });
  }

  getIcone(p: Patrimonio): string {
    if (p.matricula) return 'bi-car-front';
    if (p.morada) return 'bi-house-door';
    return 'bi-tools';
  }
}