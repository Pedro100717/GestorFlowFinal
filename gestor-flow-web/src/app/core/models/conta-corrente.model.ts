import { NumberSymbol } from "@angular/common";

export interface ContaCorrenteResumo {
    clienteId?: number;
    fornecedorId?: number;
    nome: string;
    totalFaturado: number;
    totalPago: number;
    saldoPendente: number;
}

export interface ContaCorrenteExtrato {
    dataMovimento: string;
    tipoDocumento: string;
    descricao: string;
    debito: number;
    credito: number;
    saldoAcumulado: number;
}