// 1. IMPORTAR OS MODELOS REAIS
import { Compra } from './compra.model';
import { Venda } from './venda.model';

export interface ContaBancaria {
    id?: number;
    nome: string;
    iban?: string;
    saldo: number;
    // Opcional: utilizadorId se precisares
}

export interface Movimento {
    id?: number;
    dataMovimento: string; // O Java envia datas como String ISO
    descricao: string;
    tipo: 'CREDITO' | 'DEBITO'; // Igual ao Enum do Java
    valor: number;
    saldoApos?: number;
    contaId?: number; // Para facilitar envios
    
    // 2. LIGAÇÕES 100% TIPADAS (Sem 'any')
    compraId?: number; 
    vendaId?: number; 
}