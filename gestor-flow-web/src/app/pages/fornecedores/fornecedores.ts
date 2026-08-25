import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; 
import { FornecedorService } from '../../services/fornecedor.service';
import { Fornecedor } from '../../core/models/fornecedor.model';
import { LogService } from '../../core/services/log.service'; 

import Swal from 'sweetalert2';
import * as XLSX from 'xlsx';

declare var bootstrap: Window & typeof globalThis & { Modal: any };

// 🚀 TIPAGEM RIGOROSA
export interface FornecedorPreVisualizacao extends Partial<Fornecedor> {
  linhaExcel: number;
  valido: boolean;
  erros: string[];
  selecionado: boolean;
}

@Component({
  selector: 'app-fornecedores',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './fornecedores.html',
  styleUrl: './fornecedores.scss'
})
export class FornecedoresComponent implements OnInit {

  listaFornecedores: Fornecedor[] = [];
  formFornecedor!: FormGroup;
  idEmEdicao: number | null = null;

  // --- ESTADOS DO WIZARD DE IMPORTAÇÃO ---
  passoImportacao: number = 1; 
  cabecalhosExcel: string[] = []; 
  dadosBrutosExcel: Record<string, string | number>[] = []; 
  fornecedoresPreVisualizacao: FornecedorPreVisualizacao[] = []; 
  dragOver: boolean = false; // 🚀 Controlo visual do Drag & Drop
  
  mapeamento: Record<string, string> = {
    nome: '',
    nif: '',
    email: '',
    telefone: '',
    morada: '',
    website: '' 
  };

  constructor(
    private fornecedorService: FornecedorService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();

    this.fornecedorService.fornecedores$.subscribe((fornecedores: Fornecedor[]) => {
      this.listaFornecedores = fornecedores;
      this.cd.detectChanges();
    });

    this.fornecedorService.carregarFornecedoresDaAPI();
  }

  inicializarFormulario(): void {
    this.formFornecedor = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.required, Validators.pattern('^[0-9]{9}$')]], 
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      website: [''] 
    });
  }

  get f() { return this.formFornecedor.controls; }

  // ==========================================
  // OPERAÇÕES NORMAIS (CRUD)
  // ==========================================

  abrirModalNovo(): void {
    this.idEmEdicao = null;
    this.formFornecedor.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalFornecedor')!);
    modal.show();
  }

  editarFornecedor(f: Fornecedor): void {
    this.idEmEdicao = f.id ?? null;
    this.formFornecedor.patchValue(f);
    const modal = new bootstrap.Modal(document.getElementById('modalFornecedor')!);
    modal.show();
  }

  guardarFornecedor(): void {
    if (this.formFornecedor.invalid) {
      this.formFornecedor.markAllAsTouched();
      return;
    }

    const dados = this.formFornecedor.value as Partial<Fornecedor>;

    if (this.idEmEdicao) {
      this.fornecedorService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => {
          this.logService.debug(`Fornecedor ${this.idEmEdicao} atualizado com sucesso.`);
          this.finalizar('Fornecedor atualizado!', 'modalFornecedor');
        },
        error: (e: HttpErrorResponse) => {
          this.logService.error('Falha ao atualizar fornecedor', e);
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Verifica o NIF ou os dados inseridos.',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    } else {
      this.fornecedorService.criar(dados).subscribe({
        next: () => {
          this.logService.debug('Novo fornecedor criado com sucesso.');
          this.finalizar('Fornecedor criado!', 'modalFornecedor');
        },
        error: (e: HttpErrorResponse) => {
          this.logService.error('Falha ao criar fornecedor', e);
          Swal.fire({
            icon: 'error',
            title: 'Oops...',
            text: e.error?.message || 'Não foi possível criar. NIF duplicado ou inválido?',
            confirmButtonColor: '#0d6efd'
          });
        }
      });
    }
  }

  eliminarFornecedor(id: number): void {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Este fornecedor será apagado permanentemente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.fornecedorService.apagar(id).subscribe({
          next: () => {
            this.logService.info(`Fornecedor ${id} eliminado com sucesso.`);
            Swal.fire('Eliminado!', 'O fornecedor foi apagado com sucesso.', 'success');
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Falha ao eliminar fornecedor ${id}`, e);
            Swal.fire('Erro!', 'Este fornecedor não pode ser apagado porque já tem compras associadas.', 'error');
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
    this.fornecedoresPreVisualizacao = [];
    this.dragOver = false;
    this.mapeamento = { nome: '', nif: '', email: '', telefone: '', morada: '', website: '' };
    
    const inputFicheiro = document.getElementById('inputFicheiroExcel') as HTMLInputElement;
    if (inputFicheiro) inputFicheiro.value = '';

    const modal = new bootstrap.Modal(document.getElementById('modalImportacao')!);
    modal.show();
  }

  // --- EVENTOS DO DRAG & DROP ---
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

  // --- O CLIQUE NORMAL NO INPUT ---
  lerFicheiroExcel(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.extrairDadosDoFicheiro(target.files[0]);
    }
  }

  // --- O MOTOR CENTRAL DO SHEETJS ---
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
    if (!this.mapeamento['nome'] || !this.mapeamento['nif']) {
      Swal.fire('Atenção', 'Tem de mapear obrigatoriamente as colunas do Nome e do NIF.', 'warning');
      return;
    }

    this.fornecedoresPreVisualizacao = this.dadosBrutosExcel.map((linhaAtual, index) => {
      const fornecedor: FornecedorPreVisualizacao = {
        linhaExcel: index + 2,
        valido: true,
        erros: [],
        selecionado: true
      };

      const valorNome = linhaAtual[this.mapeamento['nome']];
      if (valorNome) fornecedor.nome = String(valorNome).trim();

      const valorNif = linhaAtual[this.mapeamento['nif']];
      if (valorNif) fornecedor.nif = String(valorNif).trim();

      if (this.mapeamento['email'] && linhaAtual[this.mapeamento['email']]) 
        fornecedor.email = String(linhaAtual[this.mapeamento['email']]).trim();
      
      if (this.mapeamento['telefone'] && linhaAtual[this.mapeamento['telefone']]) 
        fornecedor.telefone = String(linhaAtual[this.mapeamento['telefone']]).trim();
      
      if (this.mapeamento['morada'] && linhaAtual[this.mapeamento['morada']]) 
        fornecedor.morada = String(linhaAtual[this.mapeamento['morada']]).trim();
        
      if (this.mapeamento['website'] && linhaAtual[this.mapeamento['website']]) 
        fornecedor.website = String(linhaAtual[this.mapeamento['website']]).trim();

      // Validação simplificada e robusta no Frontend
      if (!fornecedor.nome) {
        fornecedor.valido = false;
        fornecedor.erros.push('Nome em falta');
        fornecedor.selecionado = false;
      }
      
      if (!fornecedor.nif) {
        fornecedor.valido = false;
        fornecedor.erros.push('NIF em falta');
        fornecedor.selecionado = false;
      } else if (!/^[0-9]{9}$/.test(fornecedor.nif)) {
        fornecedor.valido = false;
        fornecedor.erros.push('NIF deve ter 9 dígitos numéricos');
        fornecedor.selecionado = false;
      }

      return fornecedor;
    });

    this.passoImportacao = 3;
  }

  gravarImportacao(): void {
    const loteLimpo: Partial<Fornecedor>[] = this.fornecedoresPreVisualizacao
      .filter(f => f.valido && f.selecionado)
      .map(f => ({
        nome: f.nome,
        nif: f.nif,
        email: f.email,
        telefone: f.telefone,
        morada: f.morada,
        website: f.website
      }));

    if (loteLimpo.length === 0) {
      Swal.fire('Erro', 'Não existem fornecedores selecionados ou válidos para importar.', 'error');
      return;
    }

    this.fornecedorService.importarEmLote(loteLimpo).subscribe({
      next: () => {
        this.logService.info(`Lote de ${loteLimpo.length} fornecedores importado.`);
        this.finalizar('Fornecedores importados com sucesso!', 'modalImportacao');
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

  finalizar(msg: string, modalId: string): void {
    const Toast = Swal.mixin({
      toast: true, position: 'top-end', showConfirmButton: false, timer: 3000
    });
    Toast.fire({ icon: 'success', title: msg });

    const modalElement = document.getElementById(modalId);
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      modal?.hide();
    }
  }
}