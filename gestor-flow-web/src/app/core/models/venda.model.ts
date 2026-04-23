export interface Venda {
    id?: number;
    dataVenda?: string;
    designacao?: string;
    totalSemIva?: number;
    totalComIva?: number;

    // 🛡️ NOVO: Estado para sabermos se está PAGO ou PENDENTE
    estadoPagamento?: string;

    // Flat Fields (Substitui os objetos antigos)
    clienteId?: number;
    clienteNome?: string;

    centroCustoId?: number;
    centroCustoCodigo?: string;

    seccaoHomoId?: number;
    seccaoHomoCodigo?: string;

    contaBancariaId?: number;
    contaBancariaNome?: string;
    
    designacaoPersonalizada?: string;

    // 🛡️ NOVO: O array de múltiplas linhas que criámos!
    linhas?: any[];

    // ⚠️ Antigos campos (Com '?' para não darem erro nas partes antigas do código)
    artigoId?: number;
    artigoNome?: string;
    quantidade?: number;
    precoUnitario?: number;
    taxaIvaId?: number;
    taxaIvaValor?: number;
}