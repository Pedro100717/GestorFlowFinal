// 🛡️ Interface de suporte para evitar o uso de 'any' nos serviços e componentes
export interface TaxaIva {
    id: number;
    valor: number;
    descricao?: string; // Opcional, para caso venha na API
}

// 🛒 O NOVO CARTÃO DE ARTIGO/SERVIÇO VENDIDO (Detail)
export interface LinhaVenda {
    id?: number;
    
    // Artigo
    artigoId: number;
    artigoNome?: string;
    
    // Matemática
    quantidade: number;
    precoUnitario: number;
    totalLinhaSemIva?: number;
    totalLinhaComIva?: number;
    
    // IVA
    taxaIvaId: number;
    taxaIvaValor?: number;
    
    // 🚀 Analítica (Mudou-se para aqui!)
    centroCustoId?: number;
    centroCustoCodigo?: string;
    seccaoHomoId?: number;
    seccaoHomoCodigo?: string;

    designacaoPersonalizada?: string;
}

// 📦 O CABEÇALHO DA FATURA DE VENDA (Master)
export interface Venda {
    id?: number;
    dataVenda?: string;
    
    // 🚀 O MOTOR DO SIMULADOR DE TESOURARIA
    dataVencimento?: string; 
    dataPrevistaPagamento?: string;
    
    totalSemIva?: number;
    totalComIva?: number;

    // 🛡️ Estado: PAGO, PENDENTE ou PARCIALMENTE_PAGO
    estadoPagamento?: string;

    // Flat Fields do Cabeçalho (Fornecedor e Tesouraria)
    clienteId?: number;
    clienteNome?: string;

    contaBancariaId?: number;
    contaBancariaNome?: string;

    planoOrigemId?: number | null;

    // 🛡️ AGORA TIPADO: O array de múltiplas linhas
    linhas?: LinhaVenda[];
}