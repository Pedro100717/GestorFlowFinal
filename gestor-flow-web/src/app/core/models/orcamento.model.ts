import { Cliente } from "./cliente.model";
import { Artigo } from "./artigo.model";

export interface LinhaOrcamento {
    id?: number;
    artigoId: number;
    artigoNome?: string; // Flat field
    
    taxaIvaId: number;
    taxaIvaValor?: number; // Flat field
    
    quantidade: number;
    precoCustoUnitario?: number;
    margemLucroPercentual?: number;
    precoVendaUnitarioOverride?: number; 
    
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
    
    totalCusto?: number;
    totalSemIva?: number;
    totalComIva?: number;
    
    // Flat Fields (sem o objeto Cliente ou Conta inteiros)
    clienteId?: number;
    clienteNome?: string;    
    
    contaBancariaId?: number; 
    contaBancariaNome?: string;
    
    linhas?: LinhaOrcamento[];
}