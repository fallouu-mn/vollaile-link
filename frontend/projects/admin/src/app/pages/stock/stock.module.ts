import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MouvementStockListComponent } from './mouvement-stock-list.component';
import { ReservationManagementComponent } from './reservation-management.component';
import { RouterModule, Routes } from '@angular/router';

// Angular Material modules
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

const routes: Routes = [
  // Route par défaut : le lien "Stock" de la barre latérale pointe sur /stock,
  // qui était une impasse (aucune route ne correspondait).
  { path: '', pathMatch: 'full', redirectTo: 'mouvements' },
  { path: 'mouvements', component: MouvementStockListComponent },
  { path: 'reservations', component: ReservationManagementComponent }
];

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    MouvementStockListComponent,
    ReservationManagementComponent,
    RouterModule.forChild(routes),
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatToolbarModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule
  ],
  exports: [
    MouvementStockListComponent,
    ReservationManagementComponent
  ]
})
export class StockModule { }