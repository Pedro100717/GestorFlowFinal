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

// ==========================================
// 🚀 MODELOS INDUSTRIAIS (PAGAMENTOS PARCIAIS)
// ==========================================

export interface DocumentoPendente {
    id: number;
    tipo: 'VENDA' | 'COMPRA';
    data: string;
    entidade: string;
    total: number;
    
    // O campo vital que o Backend agora envia para sabermos quanto falta pagar
    valorPendente: number; 
}

export interface ConfirmarPagamentoPayload {
    documentoId: number;
    tipoDocumento: 'VENDA' | 'COMPRA';
    contaBancariaId: number;
    dataPagamento?: string;
    
    // A nossa tranche obrigatória!
    valorAPagar: number; 
}

// ==========================================
// 🚀 MODELOS DO SIMULADOR DE TESOURARIA
// ==========================================

export interface PontoSimulacao {
    label: string;
    saldoProjetado: number;
}

export interface SimuladorTesourariaDTO {
    saldoAtual: number;
    pontos: PontoSimulacao[];
}