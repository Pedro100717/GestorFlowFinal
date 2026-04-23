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

// 🛡️ A NOSSA NOVA INTERFACE PARA O DASHBOARD ANALÍTICO
export interface AnaliseDashboard {
    centroCusto: string;
    seccaoHomo: string;
    totalVendas: number;
    totalCompras: number;
    margem: number;
}