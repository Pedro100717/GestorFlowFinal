export interface VendaResumo {
    cliente?: { nome: string };
    dataVenda: string;
    designacao: string;
    totalComIva: number;
  }
  
  export interface DashboardResumo {
    totalVendas: number;
    totalClientes: number;
    valorStock: number;
    totalCompras: number;
    ultimasVendas: VendaResumo[];
  }