import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StockService } from '../../services/stock.service';
import { ArtigoService } from '../../services/artigo.service';
import { ClienteService } from '../../services/cliente.service';
import { FornecedorService } from '../../services/fornecedor.service';

import { MovimentoStock } from '../../core/models/stock.model';
import { Artigo } from '../../core/models/artigo.model';
import { Cliente } from '../../core/models/cliente.model';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { forkJoin } from 'rxjs'; 

import Swal from 'sweetalert2';

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
  
  // 🛡️ VARIÁVEIS DO MODAL NATIVO
  mostrarModalHistorico: boolean = false;
  historicoArtigoSelecionado: MovimentoStock[] = [];
  artigoSelecionadoParaHistorico: string = '';
  
  listaClientes: Cliente[] = []; 
  listaFornecedores: Fornecedor[] = []; 
  
  formAcerto!: FormGroup;

  constructor(
    private stockService: StockService,
    private artigoService: ArtigoService,
    private clienteService: ClienteService,
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarDadosIniciais();

    this.stockService.historico$.subscribe((lista: MovimentoStock[]) => {
      this.listaHistorico = lista;
      this.cd.detectChanges();
    });

    this.artigoService.artigos$.subscribe((lista) => {
      this.listaMercadorias = lista.filter(a => a.movimentaStock === true || a.tipo === 'MERCADORIA');
      this.cd.detectChanges();
    });

    this.formAcerto.get('tipo')?.valueChanges.subscribe(tipoSelecionado => {
        if (tipoSelecionado === 'ENTRADA') {
            this.formAcerto.get('fornecedorId')?.setValue(null);
        } else if (tipoSelecionado === 'SAIDA') {
            this.formAcerto.get('clienteId')?.setValue(null);
        }
    });
  }

  inicializarFormulario() {
    this.formAcerto = this.fb.group({
      mercadoriaId: [null, [Validators.required]],
      tipo: ['ENTRADA', [Validators.required]],
      quantidade: [1, [Validators.required, Validators.min(0.001)]],
      motivo: ['', [Validators.required]],
      clienteId: [null],
      fornecedorId: [null]
    });
  }

  get f() { return this.formAcerto.controls; }

  carregarDadosIniciais() {
    this.stockService.carregarHistoricoDaAPI();
    this.artigoService.carregarArtigosDaAPI();
    
    forkJoin({
        clientes: this.clienteService.listar(),
        fornecedores: this.fornecedorService.listar()
    }).subscribe(res => {
        // Fallback seguro caso a API devolva paginação ou lista direta
        this.listaClientes = (res.clientes as any).content || res.clientes;
        this.listaFornecedores = (res.fornecedores as any).content || res.fornecedores;
        this.cd.detectChanges();
    });
  }

  // --- LÓGICA DO MODAL DE ACERTOS (Mantém-se o Bootstrap JS aqui porque é um form simples) ---
  abrirModalNovo(mercadoriaIdPadrao: number | null = null) {
    this.formAcerto.reset({ 
        mercadoriaId: mercadoriaIdPadrao,
        tipo: 'ENTRADA', 
        quantidade: 1, 
        clienteId: null, 
        fornecedorId: null 
    });
    const modal = new bootstrap.Modal(document.getElementById('modalAcerto'));
    modal.show();
  }

  // --- 🛡️ LÓGICA DO MODAL DE HISTÓRICO (Controlado 100% pelo Angular) ---
  abrirHistoricoArtigo(artigo: Artigo) {
    this.artigoSelecionadoParaHistorico = artigo.nome;
    this.historicoArtigoSelecionado = []; // Mostra "A carregar..." visualmente
    
    // Liga o interruptor: O Modal aparece no ecrã imediatamente
    this.mostrarModalHistorico = true; 
    
    this.stockService.obterHistoricoDoArtigo(artigo.id!).subscribe({
      next: (res: any) => {
        console.log("CHEGOU AO ANGULAR:", res);
        // Assim que o Java responde, os dados entram na variável e o Angular pinta a tabela
        this.historicoArtigoSelecionado = res.content || res || []; 

        this.cd.detectChanges(); // Garante que o Angular atualiza a view com os novos dados
      },
      error: (err: any) => {
        console.error('Erro ao buscar histórico do artigo:', err);
        Swal.fire('Erro', 'Não foi possível carregar o histórico deste artigo.', 'error');
        this.mostrarModalHistorico = false; // Fecha em caso de erro
      }
    });
  }

  fecharModalHistorico() {
    // Desliga o interruptor: O Modal desaparece
    this.mostrarModalHistorico = false;
  }
  // ----------------------------------------------------------------------

  guardarAcerto() {
    if (this.formAcerto.invalid) {
      this.formAcerto.markAllAsTouched();
      return;
    }

    const formValues = this.formAcerto.value;
    const artigo = this.listaMercadorias.find(a => a.id === formValues.mercadoriaId);

    if (formValues.tipo === 'SAIDA' && artigo && (artigo.stockAtual || 0) < formValues.quantidade) {
        Swal.fire({
          title: 'Atenção ao Stock!',
          text: `Tens apenas ${artigo.stockAtual} unidades. Queres forçar a saída de ${formValues.quantidade} unidades? O stock ficará negativo.`,
          icon: 'warning',
          showCancelButton: true,
          confirmButtonColor: '#dc3545',
          cancelButtonColor: '#6c757d',
          confirmButtonText: 'Sim, registar saída!',
          cancelButtonText: 'Cancelar'
        }).then((result) => {
          if (result.isConfirmed) this.executarRegisto(formValues);
        });
    } else {
        this.executarRegisto(formValues);
    }
  }

  private executarRegisto(formValues: any) {
    this.stockService.registarAcerto(formValues).subscribe({
      next: (novoAcerto: any) => {
        this.artigoService.atualizarStockNaMemoria(formValues.mercadoriaId, novoAcerto.stockAposMovimento); 
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: 'Acerto registado com sucesso!' });
        bootstrap.Modal.getInstance(document.getElementById('modalAcerto'))?.hide();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: 'Erro ao guardar',
          text: e.error?.message || 'Verifica os dados e tenta novamente.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }
}