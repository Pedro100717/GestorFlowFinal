import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './pages/login/login';
import { LayoutComponent } from './components/layout/layout';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { RegisterComponent } from './pages/register/register';
import { ArtigosComponent } from "./pages/artigos/artigos";
import { CentrosCustoComponent } from "./pages/analitica/centros-custo/centros-custo";
import { SeccoesHomoComponent } from "./pages/analitica/seccoes-homogeneas/seccoes-homogeneas";
// 🛡️ IMPORT DO NOVO ECRÃ DE ANÁLISE
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

export const routes: Routes = [
  // 1. Rota Vazia: Redireciona para o Login
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // 2. Página de Login (Sozinha, ecrã inteiro)
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // 3. Aplicação (Protegida)
  {
    path: 'app',
    component: LayoutComponent, // O Layout tem o Menu Lateral
    canActivate: [authGuard], // Protege TODAS as rotas-filhas
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'artigos', component: ArtigosComponent },
      { path: 'clientes', component: ClientesComponent, title: 'Gestão de Clientes'},
      { path: 'fornecedores', component: FornecedoresComponent, title: 'Gestão de Fornecedores'},
      
      // --- CONTABILIDADE ANALÍTICA ---
      { path: 'analitica/centros-custo', component: CentrosCustoComponent, title: 'Centros de Custo'},
      { path: 'analitica/seccoes-homogeneas', component: SeccoesHomoComponent, title:'Secções Homogéneas'},
      { path: 'analitica/dashboard', component: AnaliseComponent, title:'Dashboard Analítico'}, // 🛡️ NOVA ROTA AQUI!
      
      { path: 'compras', component: ComprasComponent, title: 'Gestão de Compras'},
      { path: 'vendas', component: VendasComponent, title: 'Gestão de Vendas'},
      { path: 'tesouraria', component: TesourariaComponent, title:'Tesouraria'},
      { path: 'patrimonio', component: PatrimonioComponent, title: 'Patrimonio'},
      { path: 'stock/acertos', component: AcertosStockComponent, title: 'Acertos de Stock'},
      { path: 'orcamentos', component: OrcamentosComponent, title: 'Orçamentos' },
      { path: 'tarefas', component: TarefasComponent, title: 'Tarefas' },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];