export interface TaxaIva {
    id: number;
    valor: number;
}

export interface LinhaVenda {
    id?: number;
    artigoId: number;
    artigoNome?: string;
    taxaIvaId: number;
    taxaIvaValor?: number;
    quantidade: number;
    precoUnitario: number;
    totalLinhaSemIva?: number;
    totalLinhaComIva?: number;
    designacaoPersonalizada?: string;
}

export interface Venda {
    id?: number;
    dataVenda?: string;
    
    // 🚀 O MOTOR DO SIMULADOR: Data prevista para o dinheiro entrar
    dataVencimento?: string; 
    
    designacao?: string;
    totalSemIva?: number;
    totalComIva?: number;

    // 🛡️ Estado: PAGO, PENDENTE ou PARCIALMENTE_PAGO
    estadoPagamento?: string;

    // Flat Fields (Substitui os objetos antigos)
    clienteId?: number;
    clienteNome?: string;

    centroCustoId?: number;
    centroCustoCodigo?: string;

    seccaoHomoId?: number;
    seccaoHomoCodigo?: string;

    contaBancariaId?: number;
    contaBancariaNome?: string;
    
    designacaoPersonalizada?: string;

    // 🛡️ AGORA TIPADO: O array de múltiplas linhas
    linhas?: LinhaVenda[];

    // ⚠️ Legado (Manter para compatibilidade com código antigo)
    artigoId?: number;
    artigoNome?: string;
    quantidade?: number;
    precoUnitario?: number;
    taxaIvaId?: number;
    taxaIvaValor?: number;
}