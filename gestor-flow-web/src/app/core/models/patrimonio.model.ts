export interface Patrimonio {
    id?: number;
    nome: string;
    dataAquisicao?: string;
    valorAquisicao?: number;
    
    // O Java agora envia-nos isto limpinho
    tipoPatrimonio?: string; 

    // Campos de Viatura
    matricula?: string;
    marca?: string;
    modelo?: string;
    validadeSeguro?: string;
    proximaInspecao?: string;

    // Campos de Imóvel
    morada?: string;
    artigoMatricial?: string;
    tipo?: string;

    // Campos de Ferramenta
    numeroSerie?: string;
    estadoConservacao?: string;
}

export type TipoPatrimonio = 'VIATURA' | 'IMOVEL' | 'FERRAMENTA';