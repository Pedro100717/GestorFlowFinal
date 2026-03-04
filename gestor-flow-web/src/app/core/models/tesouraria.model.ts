// 1. IMPORTAR OS MODELOS REAIS
import { Compra } from './compra.model';
import { Venda } from './venda.model';

export interface ContaBancaria {
    id?: number;
    nome: string;
    iban?: string;
    saldo: number;
}

export interface Movimento {
    id?: number;
    dataMovimento?: string; // O Java envia datas como String ISO
    descricao: string;
    tipo: 'CREDITO' | 'DEBITO';
    valor: number;
    
    // Campos usados no Input (quando criamos um movimento)
    contaId?: number;
    
    // --- Campos devolvidos pelo Response DTO (Flat Fields) ---
    compraId?: number; 
    vendaId?: number; 
    fornecedorNome?: string; // Para mostrar na tabela de tesouraria
    clienteNome?: string;    // Para mostrar na tabela de tesouraria
}