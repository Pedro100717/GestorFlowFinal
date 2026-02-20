import { Cliente } from "./cliente.model";

export type EstadoTarefa = 'PENDENTE' | 'EM_CURSO' | 'CONCLUIDA' | 'CANCELADA';
export type PrioridadeTarefa = 'BAIXA' | 'NORMAL' | 'ALTA' | 'URGENTE';

export interface Tarefa {
    id?: number;
    titulo: string;
    descricao?: string;
    estado: EstadoTarefa;
    prioridade: PrioridadeTarefa;
    dataLimite?: string;
    dataConclusao?: string;
    dataCriacao?: string;

    // Relação com Cliente (Opcional)
    clienteId?: number;
    cliente?: Cliente;
}