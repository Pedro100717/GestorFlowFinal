import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StockService } from '../../services/stock.service';
import { ArtigoService } from '../../services/artigo.service';
import { MovimentoStock } from '../../core/models/stock.model';
import { Artigo } from '../../core/models/artigo.model';

declare var bootstrap: any;

@Component({
  selector: 'app-acertos-stock',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './acertos-stock.html'
})
export class AcertosStockComponent implements OnInit {

  // Controlo das abas
  abaAtiva: 'inventario' | 'historico' = 'inventario';

  listaHistorico: MovimentoStock[] = [];
  listaMercadorias: Artigo[] = []; 
  formAcerto!: FormGroup;

  constructor(
    private stockService: StockService,
    private artigoService: ArtigoService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDados();
  }

  inicializarFormulario() {
    this.formAcerto = this.fb.group({
      mercadoriaId: [null, Validators.required],
      tipo: ['ENTRADA', Validators.required],
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      motivo: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  get f() { return this.formAcerto.controls; }

  carregarDados() {
    this.stockService.listarHistorico().subscribe({
      next: (dados: any) => {
        this.listaHistorico = dados.content || dados; 
        this.cd.detectChanges();
      }
    });

    this.artigoService.listar().subscribe({
      next: (dados: any) => {
        const todosArtigos: Artigo[] = dados.content || dados;
        this.listaMercadorias = todosArtigos.filter(a => a.movimentaStock === true);
        this.cd.detectChanges();
      }
    });
  }

  // Agora recebe opcionalmente o ID da mercadoria para pré-selecionar no formulário
  abrirModalNovo(mercadoriaId: number | null = null) {
    this.formAcerto.reset({ 
        tipo: 'ENTRADA', 
        quantidade: 1,
        mercadoriaId: mercadoriaId 
    });
    const modal = new bootstrap.Modal(document.getElementById('modalAcerto'));
    modal.show();
  }

  guardarAcerto() {
    if (this.formAcerto.invalid) {
      this.formAcerto.markAllAsTouched();
      return;
    }

    const formValues = this.formAcerto.value;
    if (formValues.tipo === 'SAIDA') {
        if(!confirm(`Confirma a saída manual de ${formValues.quantidade} unidades? O stock será deduzido.`)) return;
    }

    this.stockService.registarAcerto(formValues).subscribe({
      next: () => {
        this.carregarDados();
        bootstrap.Modal.getInstance(document.getElementById('modalAcerto'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || 'Falha ao registar.'))
    });
  }
}