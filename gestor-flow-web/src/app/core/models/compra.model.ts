// 🛡️ Interface de suporte para evitar o uso de 'any' nos serviços e componentes
export interface TaxaIva {
    id: number;
    valor: number;
    descricao?: string;
}

// 🛒 O NOVO CARTÃO DE ARTIGO (Detail)
export interface LinhaCompra {
    id?: number;
    
    // Artigo
    artigoId?: number;
    artigoNome?: string;
    
    // Matemática
    quantidade?: number; 
    precoUnitario?: number; 
    totalLinha?: number;
    
    // IVA
    taxaIvaId?: number;
    taxaIvaValor?: number;

    // Analítica
    centroCustoId?: number;
    centroCustoCodigo?: string;
    centroCustoNome?: string;

    seccaoHomoId?: number;
    seccaoHomoCodigo?: string;
    seccaoHomoNome?: string;

    designacaoPersonalizada?: string;
}

// 📦 O CABEÇALHO DA FATURA (Master)
export interface Compra {
    id?: number;
    dataCompra?: string;
    
    // 🚀 O MOTOR DO SIMULADOR
    dataVencimento?: string; 
    dataPrevistaPagamento?: string;
    
    numeroFaturaFornecedor?: string;
    total?: number; // Soma de todas as linhas

    // 🛡️ Estado: PAGO, PENDENTE ou PARCIALMENTE_PAGO
    estadoPagamento?: string;

    // Flat Fields (Fornecedor e Tesouraria)
    fornecedorId?: number;
    fornecedorNome?: string;

    contaBancariaId?: number;
    contaBancariaNome?: string;

    planoOrigemId?: number | null;

    // 🚀 A MÁGICA: A lista de artigos associada a esta fatura
    linhas?: LinhaCompra[];
}