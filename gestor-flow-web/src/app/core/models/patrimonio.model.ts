export interface Patrimonio {
    id?: number;
    nome: string;
    dataAquisicao?: string;
    valorAquisicao?: number;
    // Campos específicos opcionais (para a listagem geral funcionar)
    matricula?: string;
    marca?: string;
    modelo?: string;
    morada?: string;
    numeroSerie?: string;
}

// Tipos auxiliares para o formulário
export type TipoPatrimonio = 'VIATURA' | 'IMOVEL' | 'FERRAMENTA';