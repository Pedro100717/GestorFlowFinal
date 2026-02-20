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
    this.carregarDados();
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

  carregarDados() {
    // 1. Carregar Clientes para o Select
    this.clienteService.listar().subscribe(d => this.listaClientes = d.content || d);

    // 2. Carregar Tarefas e Distribuir pelas Colunas
    this.tarefaService.listar().subscribe(dados => {
      const todas: Tarefa[] = dados.content || dados;
      
      this.pendentes = todas.filter(t => t.estado === 'PENDENTE');
      this.emCurso = todas.filter(t => t.estado === 'EM_CURSO');
      this.concluidas = todas.filter(t => t.estado === 'CONCLUIDA');
      
      this.cd.detectChanges();
    });
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
      this.tarefaService.atualizar(this.idEmEdicao, dto).subscribe(() => this.finalizar());
    } else {
      this.tarefaService.criar(dto).subscribe(() => this.finalizar());
    }
  }

  // Mover cartão de uma coluna para outra (Ex: Pendente -> Em Curso)
  moverEstado(tarefa: Tarefa, novoEstado: EstadoTarefa) {
    this.tarefaService.mudarEstado(tarefa.id!, tarefa, novoEstado).subscribe(() => {
      this.carregarDados(); // Recarrega para mudar de coluna
    });
  }

  eliminar(id: number) {
    if(confirm('Apagar esta tarefa?')) {
        this.tarefaService.eliminar(id).subscribe(() => this.carregarDados());
    }
  }

  finalizar() {
    bootstrap.Modal.getInstance(document.getElementById('modalTarefa'))?.hide();
    this.carregarDados();
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