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
    // 🚀 A GRANDE MUDANÇA: Separar Código e Nome para o UI ficar perfeito!
    centroCustoCodigo: string;
    centroCustoNome: string;
    
    seccaoCodigo: string;
    seccaoNome: string;
    
    // Operacional (O que vai para a Tabela 1)
    totalVendasSemIva: number;
    totalComprasSemIva: number;
    margemBruta: number;
    
    // Fiscal (O que vai para a Tabela 2)
    totalIvaVendas: number;
    totalIvaCompras: number;
    saldoIva: number;
}