import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DemandeListComponent } from './list/demande-list.component';
import { DemandeCreateComponent } from './create/demande-create.component';
import { DemandeDetailComponent } from './detail/demande-detail.component';
import { DemandeStatusChangeComponent } from './status-change/demande-status-change.component';
import { RouterModule, Routes } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatGridListModule } from '@angular/material/grid-list';
import { BadgeComponent } from '../../components/badge/badge.component';

const routes: Routes = [
  { path: '', component: DemandeListComponent },
  { path: 'create', component: DemandeCreateComponent },
  { path: 'detail/:id', component: DemandeDetailComponent },
  { path: 'status-change/:id', component: DemandeStatusChangeComponent }
];

@NgModule({
  declarations: [],
  imports: [
    CommonModule,
    BadgeComponent,
    DemandeListComponent,
    DemandeCreateComponent,
    DemandeDetailComponent,
    DemandeStatusChangeComponent,
    RouterModule.forChild(routes),
    ReactiveFormsModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatToolbarModule,
    MatMenuModule,
    MatDividerModule,
    MatCardModule,
    MatListModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatGridListModule
  ]
})
export class DemandesModule { }
