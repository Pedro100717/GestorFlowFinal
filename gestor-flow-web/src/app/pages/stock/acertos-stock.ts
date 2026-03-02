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
    this.carregarDadosIniciais();

    // ESCUTAR O COFRE DO HISTÓRICO
    this.stockService.historico$.subscribe((lista: MovimentoStock[]) => {
      this.listaHistorico = lista;
      this.cd.detectChanges();
    });

    // ESCUTAR O COFRE DOS ARTIGOS
    this.artigoService.artigos$.subscribe((lista: any[]) => {
      this.listaMercadorias = lista.filter((a: any) => a.tipo === 'MERCADORIA');
      this.cd.detectChanges();
    });
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

  carregarDadosIniciais() {
    this.stockService.carregarHistoricoDaAPI();
    this.artigoService.carregarArtigosDaAPI();
  }

  abrirModalNovo(mercadoriaId: number | null = null) {
    this.formAcerto.reset({ 
        tipo: 'ENTRADA', 
        quantidade: 1,
        mercadoriaId: mercadoriaId 
    });
    new bootstrap.Modal(document.getElementById('modalAcerto')).show();
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
      next: (novoAcerto: any) => {
        // Atualiza a memória instantaneamente sem ir ao Java
        this.artigoService.atualizarStockNaMemoria(formValues.mercadoriaId, novoAcerto.stockAposMovimento); 

        alert('Acerto registado com sucesso!');
        bootstrap.Modal.getInstance(document.getElementById('modalAcerto'))?.hide();
      },
      error: (e: any) => alert('Erro: ' + (e.error?.message || 'Falha ao registar.'))
    });
  }
}