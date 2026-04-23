export interface Compra {
    id?: number;
    dataCompra?: string;
    numeroFaturaFornecedor?: string;
    designacao?: string;
    quantidade?: number; // ⚠️ Passou a opcional para evitar chatices
    precoUnitario?: number; // ⚠️ Passou a opcional
    total?: number;

    // 🛡️ NOVO: Estado para a tabela de Compras não quebrar!
    estadoPagamento?: string;

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