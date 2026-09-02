import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnaliticaService } from '../../../services/analitica.service';
import { CentroCusto } from '../../../core/models/analitica.model';
import { LogService } from '../../../core/services/log.service'; 
import Swal from 'sweetalert2';
import * as XLSX from 'xlsx';

declare var bootstrap: Window & typeof globalThis & { Modal: any };

// 🚀 TIPAGEM RIGOROSA DA PRÉ-VISUALIZAÇÃO
export interface CentroPreVisualizacao extends Partial<CentroCusto> {
  linhaExcel: number;
  valido: boolean;
  erros: string[];
  selecionado: boolean;
}

@Component({
  selector: 'app-centros-custo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], // 🚀 FormsModule injetado
  templateUrl: './centros-custo.html',
  styleUrl: './centros-custo.scss'
})
export class CentrosCustoComponent implements OnInit {

  listaCentros: CentroCusto[] = [];
  formCentro!: FormGroup;
  idEmEdicao: number | null = null;

  // --- ESTADOS DO WIZARD DE IMPORTAÇÃO ---
  passoImportacao: number = 1; 
  cabecalhosExcel: string[] = []; 
  dadosBrutosExcel: Record<string, string | number>[] = []; 
  centrosPreVisualizacao: CentroPreVisualizacao[] = []; 
  dragOver: boolean = false; 
  
  mapeamento: Record<string, string> = {
    nome: '',
    codigo: ''
  };

  constructor(
    private analiticaService: AnaliticaService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarCentros();
  }

  inicializarFormulario() {
    this.formCentro = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      codigo: ['', [Validators.required]] 
    });
  }

  get f() { return this.formCentro.controls; }

  carregarCentros() {
    this.analiticaService.listarCentros().subscribe({
      next: (dados) => {
        this.listaCentros = dados;
        this.cd.detectChanges();
        this.logService.debug('Centros de Custo carregados com sucesso.');
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar centros:', e)
    });
  }

  // ==========================================
  // OPERAÇÕES NORMAIS (CRUD)
  // ==========================================

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formCentro.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalCentro')!);
    modal.show();
  }

  editarCentro(centro: CentroCusto) {
    this.idEmEdicao = centro.id!;
    this.formCentro.patchValue({
      nome: centro.nome,
      codigo: centro.codigo
    });
    const modal = new bootstrap.Modal(document.getElementById('modalCentro')!);
    modal.show();
  }

  guardarCentro() {
    if (this.formCentro.invalid) {
      this.formCentro.markAllAsTouched();
      return;
    }

    const dto = this.formCentro.value;

    if (this.idEmEdicao) {
      this.analiticaService.atualizarCentro(this.idEmEdicao, dto).subscribe({
        next: () => this.finalizar('Centro de Custo atualizado!', 'modalCentro'),
        error: (e: HttpErrorResponse) => { 
          this.logService.error('Falha ao atualizar centro de custo', e); 
          Swal.fire({                                                     
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível atualizar o centro de custo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.analiticaService.criarCentro(dto).subscribe({
        next: () => this.finalizar('Centro de Custo criado!', 'modalCentro'),
        error: (e: HttpErrorResponse) => { 
          this.logService.error('Falha ao criar centro de custo', e); 
          Swal.fire({                                                 
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar o centro de custo.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarCentro(id: number) {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Se este centro tiver secções ou movimentos, não será apagado.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.analiticaService.eliminarCentro(id).subscribe({
          next: () => {
            Swal.fire('Eliminado!', 'O Centro de Custo foi apagado.', 'success');
            this.carregarCentros();
            this.logService.info(`Centro de custo ${id} eliminado com sucesso.`); 
          },
          error: (e: HttpErrorResponse) => { 
            this.logService.error(`Falha ao eliminar centro de custo ${id}`, e); 
            Swal.fire('Erro!', e.error?.message || 'Provavelmente este centro já está em uso noutros locais.', 'error'); 
          }
        });
      }
    });
  }

  // ==========================================
  // WIZARD DE IMPORTAÇÃO DE EXCEL / CSV
  // ==========================================

  abrirModalImportacao(): void {
    this.passoImportacao = 1;
    this.cabecalhosExcel = [];
    this.dadosBrutosExcel = [];
    this.centrosPreVisualizacao = [];
    this.dragOver = false;
    this.mapeamento = { nome: '', codigo: '' };
    
    const inputFicheiro = document.getElementById('inputFicheiroExcel') as HTMLInputElement;
    if (inputFicheiro) inputFicheiro.value = '';

    const modal = new bootstrap.Modal(document.getElementById('modalImportacao')!);
    modal.show();
  }

  aoArrastarPorCima(event: DragEvent): void {
    event.preventDefault(); 
    event.stopPropagation();
    this.dragOver = true; 
  }

  aoSairDoArrastar(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false; 
  }

  aoLargarFicheiro(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.extrairDadosDoFicheiro(files[0]);
    }
  }

  lerFicheiroExcel(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.extrairDadosDoFicheiro(target.files[0]);
    }
  }

  private extrairDadosDoFicheiro(ficheiro: File): void {
    const extensao = ficheiro.name.split('.').pop()?.toLowerCase();
    if (extensao !== 'xlsx' && extensao !== 'xls' && extensao !== 'csv') {
      Swal.fire('Formato Inválido', 'Por favor, arraste apenas ficheiros Excel ou CSV.', 'error');
      return;
    }

    const leitor: FileReader = new FileReader();
    
    leitor.onload = (e: ProgressEvent<FileReader>) => {
      if (!e.target?.result) return;
      
      const bstr: string = e.target.result as string;
      const wb: XLSX.WorkBook = XLSX.read(bstr, { type: 'binary' });

      const nomePrimeiraFolha: string = wb.SheetNames[0];
      const folha: XLSX.WorkSheet = wb.Sheets[nomePrimeiraFolha];

      this.dadosBrutosExcel = XLSX.utils.sheet_to_json<Record<string, string | number>>(folha, { defval: '' });

      if (this.dadosBrutosExcel.length > 0) {
        this.cabecalhosExcel = Object.keys(this.dadosBrutosExcel[0]);
        this.passoImportacao = 2; 
        this.cd.detectChanges();
      } else {
        Swal.fire('Erro', 'O ficheiro parece estar vazio.', 'warning');
      }
    };
    
    leitor.readAsBinaryString(ficheiro);
  }

  processarMapeamento(): void {
    // 🛡️ Apenas o Nome é estritamente obrigatório no mapeamento
    if (!this.mapeamento['nome']) {
      Swal.fire('Atenção', 'Tem de mapear obrigatoriamente a coluna do Nome.', 'warning');
      return;
    }

    // 🚀 CORREÇÃO P1-06: Barreira de segurança em TS contra colunas duplicadas
    const valoresMapeados = Object.values(this.mapeamento).filter(val => val !== '');
    const temDuplicados = new Set(valoresMapeados).size !== valoresMapeados.length;

    if (temDuplicados) {
      Swal.fire('Atenção', 'Não pode mapear a mesma coluna do ficheiro para múltiplos campos.', 'warning');
      return;
    }

    this.centrosPreVisualizacao = this.dadosBrutosExcel.map((linhaAtual, index) => {
      const centro: CentroPreVisualizacao = {
        linhaExcel: index + 2,
        valido: true,
        erros: [],
        selecionado: true
      };

      const valorNome = linhaAtual[this.mapeamento['nome']];
      if (valorNome) centro.nome = String(valorNome).trim();

      // O código pode vir vazio (o Smart Fallback do backend gera-o automaticamente)
      if (this.mapeamento['codigo']) {
        const valorCodigo = linhaAtual[this.mapeamento['codigo']];
        if (valorCodigo) centro.codigo = String(valorCodigo).trim();
      }

      if (!centro.nome) {
        centro.valido = false;
        centro.erros.push('Nome em falta');
        centro.selecionado = false;
      }

      return centro;
    });

    this.passoImportacao = 3;
  }

  gravarImportacao(): void {
    const loteLimpo: Partial<CentroCusto>[] = this.centrosPreVisualizacao
      .filter(c => c.valido && c.selecionado)
      .map(c => ({
        nome: c.nome,
        codigo: c.codigo
      }));

    if (loteLimpo.length === 0) {
      Swal.fire('Erro', 'Não existem centros selecionados ou válidos para importar.', 'error');
      return;
    }

    this.analiticaService.importarCentrosEmLote(loteLimpo).subscribe({
      next: () => {
        this.logService.info(`Lote de ${loteLimpo.length} centros de custo importado.`);
        this.finalizar('Centros importados com sucesso!', 'modalImportacao');
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Falha na importação em lote', e);
        Swal.fire('Erro na Importação', e.error?.message || 'Ocorreu um problema ao guardar.', 'error');
      }
    });
  }

  // ==========================================
  // HELPERS
  // ==========================================

  // 🚀 Adaptado para receber o ID do modal a fechar
  finalizar(msg: string, modalId: string) {
    const Toast = Swal.mixin({
      toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
    });
    Toast.fire({ icon: 'success', title: msg });

    this.carregarCentros();
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}