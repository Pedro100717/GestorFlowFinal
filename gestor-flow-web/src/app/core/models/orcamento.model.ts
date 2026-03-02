import { Cliente } from "./cliente.model";
import { Artigo } from "./artigo.model";

export interface LinhaOrcamento {
    id?: number;
    artigoId: number;
    artigo?: Artigo; // Para leitura
    
    taxaIvaId: number;
    taxaIva?: any; // Para leitura
    
    quantidade: number;
    precoCustoUnitario?: number;
    margemLucroPercentual?: number;
    precoVendaUnitarioOverride?: number; // Se o user escrever o preço final à mão
    
    // Calculados
    precoVendaUnitario?: number;
    totalLinhaSemIva?: number;
    totalLinhaComIva?: number;
}

export interface Orcamento {
    id?: number;
    dataCriacao?: string;
    dataValidade?: string;
    estado?: 'RASCUNHO' | 'ENVIADO' | 'APROVADO' | 'REJEITADO' | 'CONVERTIDO_VENDA';
    notas?: string;
    
    // --- TOTAIS (Faltava o totalComIva para o HTML) ---
    totalCusto?: number;
    totalSemIva?: number;
    totalComIva?: number;
    
    // --- DADOS DO CLIENTE ---
    cliente?: Cliente | any; // Suporta a entidade completa (o.cliente?.nome)
    clienteId?: number;
    clienteNome?: string;    // Adicionado o '?' para ser opcional
    
    // --- DADOS DA CONTA BANCÁRIA ---
    contaBancaria?: any;     // Suporta a entidade completa
    contaBancariaId?: number;

    // --- LINHAS ---
    linhas?: LinhaOrcamento[] | any[]; // Essencial para a função de editar() no .ts
}