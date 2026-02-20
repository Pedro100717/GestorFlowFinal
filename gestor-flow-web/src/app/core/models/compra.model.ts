import { Artigo } from "./artigo.model";
import { Fornecedor } from "./fornecedor.model";
import { CentroCusto, SeccaoHomo } from "./analitica.model";

export interface Compra {
    id?: number;
    dataCompra?: string;
    numeroFaturaFornecedor?: string;
    designacao?: string;
    
    quantidade: number;
    precoUnitario: number;
    total?: number;

    // --- Relações de Leitura (Vêm do Backend) ---
    fornecedor?: Fornecedor;
    artigo?: Artigo;
    centroCusto?: CentroCusto;
    seccaoHomo?: SeccaoHomo;
    
    // NOVO: O objeto Taxa de IVA completo
    taxaIva?: {
        id: number;
        descricao: string;
        valor: number;
    };

    // --- Relações de Escrita (IDs para enviar ao Backend) ---
    fornecedorId?: number;
    artigoId?: number;
    centroCustoId?: number;
    seccaoHomoId?: number;
    taxaIvaId?: number; // <--- NOVO: Obrigatório para criar a compra
}