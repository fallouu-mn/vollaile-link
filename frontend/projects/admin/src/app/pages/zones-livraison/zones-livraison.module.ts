import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZonesLivraisonListComponent } from './list/zones-livraison-list.component';
import { ZonesLivraisonFormComponent } from './form/zones-livraison-form.component';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

const routes: Routes = [
  { path: '', component: ZonesLivraisonListComponent },
  { path: 'create', component: ZonesLivraisonFormComponent },
  { path: 'edit/:id', component: ZonesLivraisonFormComponent }
];

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    ZonesLivraisonListComponent,
    ZonesLivraisonFormComponent,
    RouterModule.forChild(routes),
    ReactiveFormsModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatToolbarModule,
    MatMenuModule,
    MatDividerModule,
    MatCardModule,
    MatTooltipModule,
    MatProgressBarModule
  ]
})
export class ZonesLivraisonModule { }