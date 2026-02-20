export interface Fornecedor {
    id?: number;
    nome: string;
    nif?: string;
    email?: string;
    telefone?: string;
    morada?: string;
    website?: string; // Campo extra que Clientes não têm
}