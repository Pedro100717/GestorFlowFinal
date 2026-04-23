export interface Artigo {
    id?: number;
    nome: string;
    codigoBarras?: string;
    preco?: number;            // Preço de Venda
    ultimoPrecoCusto?: number; 
    
    // --- NOVO: Campos do DTO Plano ---
    tipo?: string;
    stockAtual?: number;
    
    familiaId?: number;
    familiaNome?: string;

    // Mantemos este para o Formulário de criação saber o que enviar ao Java
    movimentaStock?: boolean; 
}