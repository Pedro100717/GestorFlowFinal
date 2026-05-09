// 🛡️ Interface de suporte para evitar o uso de 'any' nos serviços e componentes
export interface TaxaIva {
    id: number;
    valor: number;
    descricao?: string;
}

export interface Compra {
    id?: number;
    dataCompra?: string;
    
    // 🚀 O MOTOR DO SIMULADOR: Sem isto, a simulação é apenas um palpite
    dataVencimento?: string; 
    
    numeroFaturaFornecedor?: string;
    designacao?: string;
    quantidade?: number; 
    precoUnitario?: number; 
    total?: number;

    // 🛡️ Estado: PAGO, PENDENTE ou PARCIALMENTE_PAGO
    estadoPagamento?: string;

    // Flat Fields (Campos Planos para facilitar o uso em tabelas)
    fornecedorId?: number;
    fornecedorNome?: string;

    artigoId?: number;
    artigoNome?: string;

    centroCustoId?: number;
    centroCustoCodigo?: string;

    seccaoHomoId?: number;
    seccaoHomoCodigo?: string;

    taxaIvaId?: number;
    taxaIvaValor?: number;

    contaBancariaId?: number;
    contaBancariaNome?: string;
}