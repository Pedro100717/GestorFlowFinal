export interface Artigo {
    id?: number;
    nome: string;
    codigoBarras?: string;
    preco: number;            // Preço de Venda
    
    // NOVO
    ultimoPrecoCusto?: number; 

    movimentaStock: boolean;
    stockAtual?: number;
    stockInicial?: number;

    // REMOVIDO: txIvaId e taxaIva
    // REMOVIDO: familiaId (ou manténs se quiseres usar famílias, mas no DTO Java vi que tinhas)
    familiaId?: number;
    familia?: { id: number; nome: string; };
}