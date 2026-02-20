import { Cliente } from "./cliente.model";
import { Artigo } from "./artigo.model";

export interface LinhaOrcamento {
    id?: number;
    artigoId: number;
    artigo?: Artigo; // Para leitura
    
    taxaIvaId: number;
    taxaIva?: any; // Para leitura
    
    quantidade: number;
    precoCustoUnitario?: number;
    margemLucroPercentual?: number;
    precoVendaUnitarioOverride?: number; // Se o user escrever o preço final à mão
    
    // Calculados
    precoVendaUnitario?: number;
    totalLinhaSemIva?: number;
    totalLinhaComIva?: number;
}

export interface Orcamento {
    id?: number;
    dataCriacao?: string;
    dataValidade?: string;
    estado?: 'RASCUNHO' | 'ENVIADO' | 'APROVADO' | 'REJEITADO' | 'CONVERTIDO_VENDA';
    notas?: string;
    
    clienteId: number;
    cliente?: Cliente;

    totalCusto?: number;
    totalSemIva?: number;
    totalComIva?: number;

    linhas: LinhaOrcamento[];
}