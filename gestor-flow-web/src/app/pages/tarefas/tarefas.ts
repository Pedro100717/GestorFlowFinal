import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TarefaService } from '../../services/tarefa.service';
import { ClienteService } from '../../services/cliente.service';
import { Tarefa, EstadoTarefa } from '../../core/models/tarefa.model';
import { Cliente } from '../../core/models/cliente.model';

// 1. IMPORTAR O SWEETALERT2
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-tarefas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './tarefas.html',
  styleUrl: './tarefas.scss' // Opcional se quiseres CSS específico
})
export class TarefasComponent implements OnInit {

  // Listas separadas para o Kanban
  pendentes: Tarefa[] = [];
  emCurso: Tarefa[] = [];
  concluidas: Tarefa[] = [];

  listaClientes: Cliente[] = [];
  formTarefa!: FormGroup;
  idEmEdicao: number | null = null;

  constructor(
    private tarefaService: TarefaService,
    private clienteService: ClienteService,
    private fb: FormBuilder,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.carregarClientesEInicializarTarefas();

    // A MAGIA DO KANBAN: 
    // Sempre que o Service modificar a lista (criar, alterar, mover estado),
    // isto dispara automaticamente e reorganiza as colunas em zero segundos!
    this.tarefaService.tarefas$.subscribe(todas => {
      this.pendentes = todas.filter(t => t.estado === 'PENDENTE');
      this.emCurso = todas.filter(t => t.estado === 'EM_CURSO');
      this.concluidas = todas.filter(t => t.estado === 'CONCLUIDA');
      this.cd.detectChanges();
    });
  }

  inicializarFormulario() {
    this.formTarefa = this.fb.group({
      titulo: ['', [Validators.required, Validators.minLength(3)]],
      descricao: [''],
      prioridade: ['NORMAL', Validators.required],
      estado: ['PENDENTE', Validators.required],
      dataLimite: [''],
      clienteId: [null] // Opcional
    });
  }

  carregarClientesEInicializarTarefas() {
    // 1. Carregar Clientes para o Select (Normal, sem gestão de estado)
    this.clienteService.listar().subscribe(d => {
        this.listaClientes = d.content || d;
        this.cd.detectChanges();
    });

    // 2. Manda o Serviço ir ao Java buscar as Tarefas e encher o "cofre"
    this.tarefaService.carregarTarefasDaAPI();
  }

  // --- AÇÕES ---

  abrirModalNovo() {
    this.idEmEdicao = null;
    this.formTarefa.reset({ prioridade: 'NORMAL', estado: 'PENDENTE' });
    new bootstrap.Modal(document.getElementById('modalTarefa')).show();
  }

  editar(t: Tarefa) {
    this.idEmEdicao = t.id!;
    this.formTarefa.patchValue({
      titulo: t.titulo,
      descricao: t.descricao,
      prioridade: t.prioridade,
      estado: t.estado,
      dataLimite: t.dataLimite,
      clienteId: t.clienteId
    });
    new bootstrap.Modal(document.getElementById('modalTarefa')).show();
  }

  guardar() {
    if (this.formTarefa.invalid) {
      this.formTarefa.markAllAsTouched();
      // 2. AVISO DE FORMULÁRIO INVÁLIDO
      Swal.fire({
        icon: 'warning',
        title: 'Atenção',
        text: 'Por favor, preencha o título da tarefa corretamente.',
        confirmButtonColor: '#0d6efd'
      });
      return;
    }
    const dto = this.formTarefa.value;

    const request$ = this.idEmEdicao 
      ? this.tarefaService.atualizar(this.idEmEdicao, dto)
      : this.tarefaService.criar(dto);

    // 3. TRATAMENTO DE ERROS AO GUARDAR
    request$.subscribe({
      next: () => {
        const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000 });
        Toast.fire({ icon: 'success', title: this.idEmEdicao ? 'Tarefa atualizada!' : 'Tarefa criada!' });
        this.fecharModal();
      },
      error: (e: any) => {
        Swal.fire({
          icon: 'error',
          title: 'Erro ao guardar',
          text: e.error?.message || 'Ocorreu um erro ao processar a tarefa.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }

  // Mover cartão de uma coluna para outra (Ex: Pendente -> Em Curso)
  moverEstado(tarefa: Tarefa, novoEstado: EstadoTarefa) {
    this.tarefaService.mudarEstado(tarefa.id!, tarefa, novoEstado).subscribe({
      error: () => {
        // Se a internet falhar e o serviço reverter o cartão, avisamos o utilizador!
        Swal.fire({
          icon: 'error',
          title: 'Erro de Sincronização',
          text: 'Não foi possível mover a tarefa. Verifica a tua ligação à internet.',
          confirmButtonColor: '#0d6efd'
        });
      }
    });
  }

  eliminar(id: number) {
    // 4. CONFIRMAÇÃO ELEGANTE NO KANBAN
    Swal.fire({
      title: 'Apagar Tarefa?',
      text: "Esta ação não pode ser desfeita!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545', // Vermelho
      cancelButtonColor: '#6c757d',  // Cinzento
      confirmButtonText: 'Sim, apagar!',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.tarefaService.eliminar(id).subscribe({
          next: () => {
             Swal.fire('Apagada!', 'A tarefa foi removida do quadro.', 'success');
          },
          error: () => {
             Swal.fire('Erro!', 'Não foi possível apagar a tarefa.', 'error');
          }
        });
      }
    });
  }

  fecharModal() {
    bootstrap.Modal.getInstance(document.getElementById('modalTarefa'))?.hide();
  }

  // Helper para cores das prioridades
  getCorPrioridade(p: string): string {
    switch(p) {
        case 'URGENTE': return 'text-danger fw-bold';
        case 'ALTA': return 'text-warning fw-bold';
        case 'BAIXA': return 'text-success';
        default: return 'text-primary';
    }
  }
}