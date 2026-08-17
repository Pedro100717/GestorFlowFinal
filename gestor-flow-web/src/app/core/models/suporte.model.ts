export interface BugReportDTO {
    tipo: 'BUG' | 'MELHORIA' | 'DUVIDA';
    descricao: string;
    paginaOrigem: string;
    emailUtilizador?: string;
}

export interface ReportSuporte {
    id: number;
    tipo: string;
    descricao: string;
    paginaOrigem: string;
    emailUtilizador: string;
    nomeUtilizador: string;
}