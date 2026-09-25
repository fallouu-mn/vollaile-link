import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { AuthGuard } from './auth/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  // change-password : accessible une fois connecté (token JWT présent), ou après un login
  // forcé (mustChangePassword=true). Le guard autorise cette route uniquement.
  { path: 'change-password', component: ChangePasswordComponent, canActivate: [AuthGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  // Modules fonctionnels — lazy loaded, tous protégés
  {
    path: 'clients',
    loadChildren: () => import('./pages/clients/clients.module').then(m => m.ClientsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'demandes',
    loadChildren: () => import('./pages/demandes/demandes.module').then(m => m.DemandesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'commandes',
    loadChildren: () => import('./pages/commandes/commandes.module').then(m => m.CommandesModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'stock',
    loadChildren: () => import('./pages/stock/stock.module').then(m => m.StockModule),
    canActivate: [AuthGuard]
  },
  {
    // Offres & prix : l'API existait (/api/admin/offres) mais n'était
    // atteignable depuis aucune interface.
    path: 'offres',
    loadComponent: () => import('./pages/offres/offre-list.component').then(m => m.OffreListComponent),
    canActivate: [AuthGuard]
  },
  {
    // Journal d'audit (CDC §2.5) : consultation de audit_log.
    path: 'audit',
    loadComponent: () => import('./pages/audit/audit-log.component').then(m => m.AuditLogComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'notifications',
    loadChildren: () => import('./pages/notifications/notifications.module').then(m => m.NotificationsModule),
    canActivate: [AuthGuard]
  },
  {
    path: 'zones-livraison',
    loadChildren: () => import('./pages/zones-livraison/zones-livraison.module').then(m => m.ZonesLivraisonModule),
    canActivate: [AuthGuard]
  },
  // Fallback — toute route inconnue renvoie vers la page de login
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }