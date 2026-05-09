export interface CentroCusto {
    id?: number;
    nome: string;
    codigo: string;
}

export interface SeccaoHomo {
    id?: number;
    nome: string;
    codigo: string;
}

export interface AnaliseDashboard {
    centroCusto: string;
    seccaoHomo: string;
    
    // Operacional (O que vai para a Tabela 1)
    totalVendasSemIva: number;
    totalComprasSemIva: number;
    margemBruta: number;
    
    // Fiscal (O que vai para a Tabela 2)
    totalIvaVendas: number;
    totalIvaCompras: number;
    saldoIva: number;
}