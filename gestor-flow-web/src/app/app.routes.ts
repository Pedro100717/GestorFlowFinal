import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { superAdminGuard } from './core/guards/super-admin.guard';

import { LoginComponent } from './pages/login/login';
import { LayoutComponent } from './components/layout/layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { RegisterComponent } from './pages/register/register';
import { ArtigosComponent } from "./pages/artigos/artigos";
import { CentrosCustoComponent } from "./pages/analitica/centros-custo/centros-custo";
import { SeccoesHomoComponent } from "./pages/analitica/seccoes-homogeneas/seccoes-homogeneas";
import { AnaliseComponent } from "./pages/analitica/analise/analise.component";
import { ClientesComponent }  from './pages/clientes/clientes';
import { FornecedoresComponent } from './pages/fornecedores/fornecedores';
import { ComprasComponent } from './pages/compras/compras';
import { VendasComponent } from './pages/venda/venda';
import { TesourariaComponent } from './pages/tesouraria/tesouraria';
import { PatrimonioComponent } from './pages/patrimonio/patrimonio';
import { AcertosStockComponent } from './pages/stock/acertos-stock';
import { OrcamentosComponent } from './pages/orcamentos/orcamentos';
import { TarefasComponent } from './pages/tarefas/tarefas';

import { ContaCorrenteClientesComponent } from './pages/conta-corrente/clientes/conta-corrente-clientes';
import { ContaCorrenteFornecedoresComponent } from './pages/conta-corrente/fornecedores/conta-corrente-fornecedores';
import { DefinicoesComponent } from './pages/definicoes/definicoes';

// 🚀 IMPORT DO NOVO COMPONENTE DO BACKOFFICE (Ajusta o caminho conforme a tua pasta)
import { AdminSuporteComponent } from './pages/admin/suporte/admin-suporte';

export const routes: Routes = [
  // 1. Rotas de Entrada
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, title: 'GestorFlow - Login' },
  { path: 'register', component: RegisterComponent, title: 'GestorFlow - Registo' },

  // 2. Aplicação Principal (Protegida pelo Layout e AuthGuard)
  {
    path: 'app',
    component: LayoutComponent, 
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, title: 'Dashboard' },
      
      // 🚀 ROTA DO BACKOFFICE (Duplamente protegida)
      { 
        path: 'admin/suporte', 
        component: AdminSuporteComponent, 
        canActivate: [superAdminGuard], 
        title: 'GestorFlow - Backoffice Suporte' 
      },

      { path: 'artigos', component: ArtigosComponent, title: 'Gestão de Artigos' },
      { path: 'clientes', component: ClientesComponent, title: 'Gestão de Clientes' },
      { path: 'fornecedores', component: FornecedoresComponent, title: 'Gestão de Fornecedores' },
      
      // --- CONTABILIDADE ANALÍTICA ---
      { path: 'analitica/centros-custo', component: CentrosCustoComponent, title: 'Centros de Custo' },
      { path: 'analitica/seccoes-homogeneas', component: SeccoesHomoComponent, title: 'Secções Homogéneas' },
      { path: 'analitica/dashboard', component: AnaliseComponent, title: 'Dashboard Analítico' },
      
      // --- OPERACIONAL ---
      { path: 'compras', component: ComprasComponent, title: 'Gestão de Compras' },
      { path: 'vendas', component: VendasComponent, title: 'Gestão de Vendas' },
      { path: 'tesouraria', component: TesourariaComponent, title: 'Tesouraria' },
      { path: 'patrimonio', component: PatrimonioComponent, title: 'Património' },
      { path: 'stock/acertos', component: AcertosStockComponent, title: 'Acertos de Stock' },
      { path: 'orcamentos', component: OrcamentosComponent, title: 'Orçamentos' },
      { path: 'tarefas', component: TarefasComponent, title: 'Gestão de Tarefas' },

      // --- CONTAS CORRENTES ---
      { 
        path: 'contas-correntes/clientes', 
        component: ContaCorrenteClientesComponent, 
        title: 'C.C. Clientes' 
      },
      { 
        path: 'contas-correntes/fornecedores', 
        component: ContaCorrenteFornecedoresComponent, 
        title: 'C.C. Fornecedores' 
      },

      // --- DEFINIÇÕES ---
      { 
        path: 'definicoes', 
        component: DefinicoesComponent, 
        title: 'Definições da Conta' 
      },

      // 🛡️ REDIREÇÃO PADRÃO (Sempre no fim para não interceptar rotas específicas)
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ]
  },

  // 3. Rota de Fuga (Opcional: Redireciona caminhos inexistentes para o login)
  { path: '**', redirectTo: 'login' }
];