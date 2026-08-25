import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ClienteService } from '../../services/cliente.service';
import { Cliente } from '../../core/models/cliente.model';
import { LogService } from '../../core/services/log.service';

import Swal from 'sweetalert2';
import * as XLSX from 'xlsx';

declare var bootstrap: Window & typeof globalThis & { Modal: any };

// 🚀 TIPAGEM RIGOROSA COM A CHECKBOX DE SELEÇÃO
export interface ClientePreVisualizacao extends Partial<Cliente> {
  linhaExcel: number;
  valido: boolean;
  erros: string[];
  selecionado: boolean;
}

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './clientes.html',
  styleUrl: './clientes.scss'
})
export class ClientesComponent implements OnInit {

  listaClientes: Cliente[] = [];
  formCliente!: FormGroup;
  idEmEdicao: number | null = null;

  // --- ESTADOS DO WIZARD DE IMPORTAÇÃO ---
  passoImportacao: number = 1; 
  cabecalhosExcel: string[] = []; 
  dadosBrutosExcel: Record<string, string | number>[] = []; 
  clientesPreVisualizacao: ClientePreVisualizacao[] = []; 
  dragOver: boolean = false; // 🚀 Controlo visual do Drag & Drop
  
  mapeamento: Record<string, string> = {
    nome: '',
    nif: '',
    email: '',
    telefone: '',
    morada: ''
  };

  constructor(
    private clienteService: ClienteService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef,
    private logService: LogService
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();

    this.clienteService.clientes$.subscribe((clientes: Cliente[]) => {
      this.listaClientes = clientes;
      this.cd.detectChanges();
    });

    this.clienteService.carregarClientesDaAPI();
  }

  inicializarFormulario(): void {
    this.formCliente = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      nif: ['', [Validators.required, Validators.pattern('^[0-9]{9}$')]],
      email: ['', [Validators.email]],
      telefone: [''],
      morada: [''],
      anotacoes: ['']
    });
  }

  get f() { return this.formCliente.controls; }

  // ==========================================
  // OPERAÇÕES NORMAIS (CRUD)
  // ==========================================

  abrirModalNovo(): void {
    this.idEmEdicao = null;
    this.formCliente.reset();
    const modal = new bootstrap.Modal(document.getElementById('modalCliente')!);
    modal.show();
  }

  editarCliente(cliente: Cliente): void {
    this.idEmEdicao = cliente.id ?? null;
    this.formCliente.patchValue(cliente); 
    const modal = new bootstrap.Modal(document.getElementById('modalCliente')!);
    modal.show();
  }

  guardarCliente(): void {
    if (this.formCliente.invalid) {
      this.formCliente.markAllAsTouched();
      return;
    }

    const dados = this.formCliente.value as Partial<Cliente>;

    if (this.idEmEdicao) {
      this.clienteService.atualizar(this.idEmEdicao, dados).subscribe({
        next: () => {
          this.logService.debug(`Cliente ${this.idEmEdicao} atualizado.`);
          this.finalizar('Cliente atualizado!', 'modalCliente');
        },
        error: (e: HttpErrorResponse) => this.tratarErro('atualizar', e)
      });
    } else {
      this.clienteService.criar(dados).subscribe({
        next: () => {
          this.logService.debug('Novo cliente criado.');
          this.finalizar('Cliente criado!', 'modalCliente');
        },
        error: (e: HttpErrorResponse) => this.tratarErro('criar', e)
      });
    }
  }

  eliminarCliente(id: number): void {
    Swal.fire({
      title: 'Tem a certeza?',
      text: "Isto apagará o histórico deste cliente!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', 
      cancelButtonColor: '#6c757d',  
      confirmButtonText: 'Sim, eliminar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.clienteService.apagar(id).subscribe({
          next: () => {
            this.logService.info(`Cliente ${id} eliminado.`);
            Swal.fire('Eliminado!', 'O cliente foi apagado.', 'success');
          },
          error: (e: HttpErrorResponse) => {
            this.logService.error(`Falha ao eliminar cliente ${id}`, e);
            Swal.fire('Erro!', 'Este cliente não pode ser apagado porque já tem vendas.', 'error');
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
    this.clientesPreVisualizacao = [];
    this.dragOver = false;
    this.mapeamento = { nome: '', nif: '', email: '', telefone: '', morada: '' };
    
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

    this.clientesPreVisualizacao = this.dadosBrutosExcel.map((linhaAtual, index) => {
      const cliente: ClientePreVisualizacao = {
        linhaExcel: index + 2,
        valido: true,
        erros: [],
        selecionado: true
      };

      const valorNome = linhaAtual[this.mapeamento['nome']];
      if (valorNome) cliente.nome = String(valorNome).trim();

      const valorNif = linhaAtual[this.mapeamento['nif']];
      if (valorNif) cliente.nif = String(valorNif).trim();

      if (this.mapeamento['email'] && linhaAtual[this.mapeamento['email']]) 
        cliente.email = String(linhaAtual[this.mapeamento['email']]).trim();
      
      if (this.mapeamento['telefone'] && linhaAtual[this.mapeamento['telefone']]) 
        cliente.telefone = String(linhaAtual[this.mapeamento['telefone']]).trim();
      
      if (this.mapeamento['morada'] && linhaAtual[this.mapeamento['morada']]) 
        cliente.morada = String(linhaAtual[this.mapeamento['morada']]).trim();

      // Regras de negócio (Simplificadas para o frontend)
      if (!cliente.nome) {
        cliente.valido = false;
        cliente.erros.push('Nome em falta');
        cliente.selecionado = false;
      }
      
      if (!cliente.nif) {
        cliente.valido = false;
        cliente.erros.push('NIF em falta');
        cliente.selecionado = false;
      } else if (!/^[0-9]{9}$/.test(cliente.nif)) {
        cliente.valido = false;
        cliente.erros.push('NIF deve ter 9 dígitos numéricos');
        cliente.selecionado = false;
      }

      return cliente;
    });

    this.passoImportacao = 3;
  }

  gravarImportacao(): void {
    const loteLimpo: Partial<Cliente>[] = this.clientesPreVisualizacao
      .filter(c => c.valido && c.selecionado)
      .map(c => ({
        nome: c.nome,
        nif: c.nif,
        email: c.email,
        telefone: c.telefone,
        morada: c.morada
      }));

    if (loteLimpo.length === 0) {
      Swal.fire('Erro', 'Não existem clientes selecionados ou válidos para importar.', 'error');
      return;
    }

    this.clienteService.importarEmLote(loteLimpo).subscribe({
      next: () => {
        this.logService.info(`Lote de ${loteLimpo.length} clientes importado.`);
        this.finalizar('Clientes importados com sucesso!', 'modalImportacao');
      },
      error: (e: HttpErrorResponse) => {
        this.logService.error('Falha na importação em lote', e);
        Swal.fire('Erro na Importação', e.error?.message || 'Ocorreu um problema ao guardar.', 'error');
      }
    });
  }

  private tratarErro(acao: string, e: HttpErrorResponse): void {
    this.logService.error(`Falha ao ${acao} cliente`, e);
    Swal.fire({
      icon: 'error',
      title: 'Oops...',
      text: e.error?.message || 'Verifica os dados inseridos.',
      confirmButtonColor: '#0d6efd'
    });
  }

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