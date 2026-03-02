import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PatrimonioService } from '../../services/patrimonio.service';
import { Patrimonio, TipoPatrimonio } from '../../core/models/patrimonio.model';

declare var bootstrap: any;

@Component({
  selector: 'app-patrimonio',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './patrimonio.html'
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
        alert('Ativo criado com sucesso!');
        bootstrap.Modal.getInstance(document.getElementById('modalPatrimonio'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || e.message))
    });
  }

  eliminar(id: number) {
    if(confirm('Tem a certeza que deseja eliminar este ativo?')) {
        this.patrimonioService.eliminar(id).subscribe({
          next: () => {
            // Removido da tabela automaticamente pelo serviço
          },
          error: (e: any) => alert('Erro ao eliminar ativo.')
        });
    }
  }

  getIcone(p: Patrimonio): string {
    if (p.matricula) return 'bi-car-front';
    if (p.morada) return 'bi-house-door';
    return 'bi-tools';
  }
}