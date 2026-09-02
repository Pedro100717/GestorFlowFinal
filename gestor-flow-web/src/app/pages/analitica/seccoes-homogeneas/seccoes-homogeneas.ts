import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AnaliticaService } from '../../../services/analitica.service';
import { SeccaoHomo } from '../../../core/models/analitica.model'; 
import { LogService } from '../../../core/services/log.service'; 

import Swal from 'sweetalert2';
import * as XLSX from 'xlsx';

declare var bootstrap: Window & typeof globalThis & { Modal: any };

// 🚀 TIPAGEM RIGOROSA DA PRÉ-VISUALIZAÇÃO
export interface SeccaoPreVisualizacao extends Partial<SeccaoHomo> {
  linhaExcel: number;
  valido: boolean;
  erros: string[];
  selecionado: boolean;
}

@Component({
  selector: 'app-seccoes-homogeneas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], // 🚀 FormsModule injetado
  templateUrl: './seccoes-homogeneas.html',
  styleUrl: './seccoes-homogeneas.scss'
})
export class SeccoesHomoComponent implements OnInit {

  listaSeccoes: SeccaoHomo[] = [];
  formSeccao!: FormGroup;
  idEmEdicao: number | null = null;

  // --- ESTADOS DO WIZARD DE IMPORTAÇÃO ---
  passoImportacao: number = 1; 
  cabecalhosExcel: string[] = []; 
  dadosBrutosExcel: Record<string, string | number>[] = []; 
  seccoesPreVisualizacao: SeccaoPreVisualizacao[] = []; 
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
      next: (dados: any) => { 
        this.listaSeccoes = dados.content || dados;
        this.cd.detectChanges();
        this.logService.debug('Secções Homogéneas carregadas com sucesso.'); 
      },
      error: (e: HttpErrorResponse) => this.logService.error('Erro ao carregar secções:', e) 
    });
  }

  // ==========================================
  // OPERAÇÕES NORMAIS (CRUD)
  // ==========================================

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formSeccao.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalSeccao')!);
    modal.show();
  }

  editarSeccao(seccao: SeccaoHomo) {
    this.idEmEdicao = seccao.id!;
    this.formSeccao.patchValue({
      nome: seccao.nome,
      codigo: seccao.codigo
    });
    const modal = new bootstrap.Modal(document.getElementById('modalSeccao')!);
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
        next: () => this.finalizar('Secção atualizada!', 'modalSeccao'),
        error: (e: HttpErrorResponse) => {
          this.logService.error('Falha ao atualizar secção', e); 
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
        next: () => this.finalizar('Secção criada com sucesso!', 'modalSeccao'),
        error: (e: HttpErrorResponse) => { 
          this.logService.error('Falha ao criar secção', e); 
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
            this.logService.info(`Secção homogénea ${id} eliminada com sucesso.`); 
          },
          error: (e: HttpErrorResponse) => { 
            this.logService.error(`Falha ao eliminar secção ${id}`, e); 
            Swal.fire('Erro!', e.error?.message || 'Não foi possível eliminar esta secção. Pode estar em uso.', 'error');
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
    this.seccoesPreVisualizacao = [];
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

    this.seccoesPreVisualizacao = this.dadosBrutosExcel.map((linhaAtual, index) => {
      const seccao: SeccaoPreVisualizacao = {
        linhaExcel: index + 2,
        valido: true,
        erros: [],
        selecionado: true
      };

      const valorNome = linhaAtual[this.mapeamento['nome']];
      if (valorNome) seccao.nome = String(valorNome).trim();

      // O código pode vir vazio (o Smart Fallback do backend gera-o automaticamente)
      if (this.mapeamento['codigo']) {
        const valorCodigo = linhaAtual[this.mapeamento['codigo']];
        if (valorCodigo) seccao.codigo = String(valorCodigo).trim();
      }

      if (!seccao.nome) {
        seccao.valido = false;
        seccao.erros.push('Nome em falta');
        seccao.selecionado = false;
      }

      return seccao;
    });

    this.passoImportacao = 3;
  }

  gravarImportacao(): void {
    const loteLimpo: Partial<SeccaoHomo>[] = this.seccoesPreVisualizacao
      .filter(s => s.valido && s.selecionado)
      .map(s => ({
        nome: s.nome,
        codigo: s.codigo
      }));

    if (loteLimpo.length === 0) {
      Swal.fire('Erro', 'Não existem secções selecionadas ou válidas para importar.', 'error');
      return;
    }

    this.analiticaService.importarSeccoesEmLote(loteLimpo).subscribe({
      next: () => {
        this.logService.info(`Lote de ${loteLimpo.length} secções importado.`);
        this.finalizar('Secções importadas com sucesso!', 'modalImportacao');
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

  finalizar(msg: string, modalId: string) {
    const Toast = Swal.mixin({
      toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
    });
    Toast.fire({ icon: 'success', title: msg });

    this.carregarDados();
    
    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      bootstrap.Modal.getInstance(modalElement)?.hide();
    }
  }
}