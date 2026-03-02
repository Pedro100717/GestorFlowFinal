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

    // Flat Fields (Substitui os objetos antigos)
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