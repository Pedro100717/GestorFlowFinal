export interface VendaResumo {
  // 1. Substituímos o objeto antigo pelo Flat Field do Java
  clienteNome?: string; 
  
  dataVenda: string;
  designacao: string;
  totalComIva: number;
}

export interface DashboardResumo {
  // 2. Isto mantém-se 100% igual, está perfeito!
  totalVendas: number;
  totalClientes: number;
  valorStock: number;
  totalCompras: number;
  ultimasVendas: VendaResumo[];
}