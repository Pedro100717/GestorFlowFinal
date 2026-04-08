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
    
    // Para leitura (Agora achatado/flat, igual ao DTO do Java!)
    centroCustoNome?: string; 
}