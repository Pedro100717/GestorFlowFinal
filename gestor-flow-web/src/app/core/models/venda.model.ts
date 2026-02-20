import { Artigo } from "./artigo.model";
import { Cliente } from "./cliente.model";
import { CentroCusto, SeccaoHomo } from "./analitica.model";

export interface Venda {
    id?: number;
    dataVenda?: string;
    designacao?: string;
    
    quantidade: number;
    precoUnitario: number;
    
    // Valores calculados pelo Backend
    totalSemIva?: number;
    totalComIva?: number; // O valor final que o cliente paga

    // Relações de Leitura
    cliente?: Cliente;
    artigo?: Artigo;
    centroCusto?: CentroCusto;
    seccaoHomo?: SeccaoHomo;
    
    taxaIva?: {
        id: number;
        descricao: string;
        valor: number;
    };

    // Relações de Escrita (IDs para o formulário)
    clienteId?: number;
    artigoId?: number;
    centroCustoId?: number;
    seccaoHomoId?: number;
    taxaIvaId?: number;
    
    designacaoPersonalizada?: string;
}