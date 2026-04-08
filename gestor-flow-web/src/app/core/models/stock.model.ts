import { Artigo } from "./artigo.model";

export interface MovimentoStock {
    id?: number;
    dataMovimento?: string;
    tipo: 'ENTRADA' | 'SAIDA';
    quantidade: number;
    motivo: string;
    stockAposMovimento?: number; // Calculado pelo backend
    
    // Para envio no DTO de criação
    mercadoriaId?: number;
    mercadoriaNome?: string;
}