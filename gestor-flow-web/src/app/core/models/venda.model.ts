import { Artigo } from "./artigo.model";
import { Cliente } from "./cliente.model";
import { CentroCusto, SeccaoHomo } from "./analitica.model";

export interface Venda {
    id?: number;
    dataVenda?: string;
    designacao?: string;
    quantidade: number;
    precoUnitario: number;
    totalSemIva?: number;
    totalComIva?: number;

    // Flat Fields (Substitui os objetos antigos)
    clienteId?: number;
    clienteNome?: string;

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
    
    designacaoPersonalizada?: string;
}