import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CommandeListComponent } from './list/commande-list.component';
import { CommandeDetailComponent } from './detail/commande-detail.component';
import { RouterModule, Routes } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ReactiveFormsModule } from '@angular/forms';
import { BadgeComponent } from '../../components/badge/badge.component';

const routes: Routes = [
  { path: '', component: CommandeListComponent },
  { path: 'detail/:id', component: CommandeDetailComponent }
];

@NgModule({
  declarations: [
    CommandeListComponent,
    CommandeDetailComponent
  ],
  imports: [
    CommonModule,
    BadgeComponent,
    RouterModule.forChild(routes),
    MatTableModule,
    MatPaginatorModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatListModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule
  ]
})
export class CommandesModule { }