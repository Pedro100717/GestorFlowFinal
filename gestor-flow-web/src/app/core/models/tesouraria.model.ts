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
    fornecedorNome?: string; 
    clienteNome?: string;    
}

// ==========================================
// 🚀 MODELOS INDUSTRIAIS (PAGAMENTOS PARCIAIS)
// ==========================================

export interface DocumentoPendente {
    id: number;
    tipo: 'VENDA' | 'COMPRA' | 'RECEITA' | 'DESPESA'; // 🚀 ATUALIZADO
    descricao?: string;
    data: string;
    entidade: string;
    total: number;
    valorPendente: number; 
}

export interface ConfirmarPagamentoPayload {
    documentoId: number;
    tipoDocumento: 'VENDA' | 'COMPRA' | 'RECEITA' | 'DESPESA'; // 🚀 ATUALIZADO
    contaBancariaId: number;
    dataPagamento?: string;
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

// ==========================================
// 🚀 MODELOS DE PLANEAMENTO FINANCEIRO (CASH FLOW PURO)
// ==========================================

export enum TipoMovimentoPlaneado {
    ENTRADA = 'ENTRADA',
    SAIDA = 'SAIDA'
}

export enum FrequenciaMovimento {
    PONTUAL = 'PONTUAL',
    SEMANAL = 'SEMANAL',
    MENSAL = 'MENSAL',
    TRIMESTRAL = 'TRIMESTRAL',
    SEMESTRAL = 'SEMESTRAL',
    ANUAL = 'ANUAL'
}

export interface MovimentoPlaneado {
    id?: number;
    descricao: string;
    tipo: string; // ou TipoMovimentoPlaneado
    frequencia: string; // ou FrequenciaMovimento
    valorBase: number;
    dataInicio: string;
    dataFim?: string;
    ativo?: boolean;
    clienteId?: number;
    fornecedorId?: number;
    dataUltimoProcessamento?: string;
    
    // 🚀 A MÁQUINA DO TEMPO: O Angular agora já sabe que isto existe!
    datasIgnoradas?: string[]; 
}