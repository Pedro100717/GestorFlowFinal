export interface CentroCusto {
    id?: number;
    nome: string;
    codigo: string;
}

export interface SeccaoHomo {
    id?: number;
    nome: string;
    codigo: string;
    
    // Para escrita (enviar para o Backend)
    centroCustoId: number;
    
    // Para leitura (receber do Backend - o objeto completo)
    centroCusto?: CentroCusto;
}