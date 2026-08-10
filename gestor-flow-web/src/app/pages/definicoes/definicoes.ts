import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // 🚀 IMPORT DOS ERROS
import { DefinicoesService, PerfilUtilizadorDTO, EmpresaDTO } from '../../services/definicoes.service';
import { LogService } from '../../core/services/log.service'; // 🚀 O INSPETOR
import { environment } from '../../../environments/environment'; // 🚀 O SALVA-VIDAS DO LOCALHOST
import Swal from 'sweetalert2';

@Component({
  selector: 'app-definicoes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './definicoes.html'
})
export class DefinicoesComponent implements OnInit {

  formPerfil!: FormGroup;
  formEmpresa!: FormGroup;

  isCarregando: boolean = true;
  isGuardandoPerfil: boolean = false;
  isGuardandoEmpresa: boolean = false;

  logoAtual: string | undefined = undefined;

  constructor(
    private fb: FormBuilder,
    private definicoesService: DefinicoesService,
    private cdr: ChangeDetectorRef,
    private logService: LogService // 🚀 INJETADO AQUI
  ) {}

  ngOnInit(): void {
    this.inicializarFormularios();
    this.carregarDados();
  }

  private inicializarFormularios(): void {
    this.formPerfil = this.fb.group({
      nome: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]]
    });

    this.formEmpresa = this.fb.group({
      nomeFiscal: ['', [Validators.required, Validators.maxLength(255)]],
      nif: ['', [
        Validators.required, 
        Validators.minLength(9), 
        Validators.maxLength(9), 
        Validators.pattern('^[0-9]{9}$') 
      ]],
      moradaCompleta: ['', [Validators.required]],
      codigoPostal: ['', [Validators.required, Validators.maxLength(20)]],
      localidade: ['', [Validators.required, Validators.maxLength(100)]],
      telefone: ['', [Validators.required, Validators.maxLength(50)]],
      emailGeral: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });
  }

  private carregarDados(): void {
    this.isCarregando = true;

    this.definicoesService.obterPerfil().subscribe({
      next: (perfil: PerfilUtilizadorDTO) => {
        this.formPerfil.patchValue(perfil);
        this.logService.debug('Perfil de utilizador carregado.'); // 🚀 RASTREABILIDADE
        this.verificarFimCarregamento();
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error('Erro ao carregar perfil', err); // 🚀 CAIXA NEGRA
        this.verificarFimCarregamento();
      }
    });

    this.definicoesService.obterEmpresa().subscribe({
      next: (empresa: EmpresaDTO) => {
        this.formEmpresa.patchValue(empresa);
        this.logoAtual = empresa.logotipoPath; 
        this.logService.debug('Dados da empresa carregados.'); // 🚀 RASTREABILIDADE
        this.verificarFimCarregamento();
      },
      error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
        this.logService.error('Erro ao carregar os dados da empresa', err); // 🚀 CAIXA NEGRA
        this.verificarFimCarregamento();
      }
    });
  }

  private verificarFimCarregamento(): void {
    this.isCarregando = false;
    this.cdr.detectChanges(); 
  }

  onLogoSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        Swal.fire('Formato Inválido', 'Por favor escolha uma imagem (JPG, PNG).', 'error');
        return;
      }

      this.isCarregando = true;
      this.cdr.detectChanges();

      this.definicoesService.uploadLogo(file).subscribe({
        next: (res) => {
          this.logoAtual = res.caminho; 
          this.logService.info('Logótipo da empresa atualizado com sucesso.'); // 🚀 RASTREABILIDADE
          Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Logótipo atualizado!', timer: 3000, showConfirmButton: false });
          this.verificarFimCarregamento();
        },
        error: (err: HttpErrorResponse) => { // 🚀 TIPAGEM ESTRITA
          this.logService.error('Falha no upload do logótipo', err); // 🚀 CAIXA NEGRA PRIMEIRO
          Swal.fire('Erro', 'Ocorreu um erro ao enviar a imagem.', 'error'); // 🚀 UX DEPOIS
          this.verificarFimCarregamento();
        }
      });
    }
  }

  getLogoUrl(path: string): string {
    // 🚀 A CORREÇÃO DE OURO: Substituímos o localhost pelo endereço real da API
    // Assumindo que a tua API_URL termina em "/api", tiramos isso para apanhar os ficheiros estáticos
    const baseUrl = environment.apiUrl.replace('/api', ''); 
    return `${baseUrl}${path}`;
  }

  guardarPerfil(): void {
    if (this.formPerfil.invalid) {
      this.formPerfil.markAllAsTouched();
      return;
    }

    this.isGuardandoPerfil = true;
    this.definicoesService.atualizarPerfil(this.formPerfil.value).subscribe({
      next: (res) => {
        this.isGuardandoPerfil = false;
        if (res && res.nome) {
           localStorage.setItem('userName', res.nome);
           localStorage.setItem('userEmail', res.email);
        }
        this.logService.debug('Perfil atualizado e guardado no LocalStorage.'); // 🚀 RASTREABILIDADE
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Perfil atualizado!', timer: 3000, showConfirmButton: false });
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.isGuardandoPerfil = false;
        this.logService.error('Erro ao gravar dados do perfil', err); // 🚀 CAIXA NEGRA
        const msg = err.error?.message || 'Erro ao atualizar perfil.';
        Swal.fire('Erro', msg, 'error');
        this.cdr.detectChanges();
      }
    });
  }

  guardarEmpresa(): void {
    if (this.formEmpresa.invalid) {
      this.formEmpresa.markAllAsTouched();
      return;
    }

    this.isGuardandoEmpresa = true;
    this.definicoesService.atualizarEmpresa(this.formEmpresa.value).subscribe({
      next: () => {
        this.isGuardandoEmpresa = false;
        this.logService.info('Configurações da empresa atualizadas.'); // 🚀 RASTREABILIDADE
        Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Dados da Empresa atualizados!', timer: 3000, showConfirmButton: false });
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.isGuardandoEmpresa = false;
        this.logService.error('Erro ao gravar dados da empresa', err); // 🚀 CAIXA NEGRA
        const msg = err.error?.message || 'Erro ao atualizar dados da empresa.';
        Swal.fire('Erro', msg, 'error');
        this.cdr.detectChanges();
      }
    });
  }
}