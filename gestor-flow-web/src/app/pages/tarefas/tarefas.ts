import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TarefaService } from '../../services/tarefa.service';
import { ClienteService } from '../../services/cliente.service';
import { Tarefa, EstadoTarefa } from '../../core/models/tarefa.model';
import { Cliente } from '../../core/models/cliente.model';

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
      clienteId: t.cliente?.id
    });
    new bootstrap.Modal(document.getElementById('modalTarefa')).show();
  }

  guardar() {
    if (this.formTarefa.invalid) {
      this.formTarefa.markAllAsTouched();
      return;
    }
    const dto = this.formTarefa.value;

    if (this.idEmEdicao) {
      this.tarefaService.atualizar(this.idEmEdicao, dto).subscribe(() => this.fecharModal());
    } else {
      this.tarefaService.criar(dto).subscribe(() => this.fecharModal());
    }
  }

  // Mover cartão de uma coluna para outra (Ex: Pendente -> Em Curso)
  moverEstado(tarefa: Tarefa, novoEstado: EstadoTarefa) {
    // Já NÃO FAZEMOS o carregarDados()! O tap() no service vai atirar a tarefa para a coluna certa na memória.
    this.tarefaService.mudarEstado(tarefa.id!, tarefa, novoEstado).subscribe();
  }

  eliminar(id: number) {
    if(confirm('Apagar esta tarefa?')) {
        // Já NÃO FAZEMOS o carregarDados()! O tap() vai tirar a tarefa do ecrã instantaneamente.
        this.tarefaService.eliminar(id).subscribe();
    }
  }

  fecharModal() {
    bootstrap.Modal.getInstance(document.getElementById('modalTarefa'))?.hide();
    // Adeus recarregamento constante de listas!
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