import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ZoneLivraisonService } from '../../../services/zone-livraison.service';
import { ZoneLivraisonDTO } from '../../../dto/zone-livraison.dto';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-zones-livraison-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './zones-livraison-list.component.html',
  styleUrls: ['./zones-livraison-list.component.scss']
})
export class ZonesLivraisonListComponent implements OnInit {
  zones: ZoneLivraisonDTO[] = [];
  displayedColumns: string[] = ['id', 'nom', 'code', 'fraisLivraison', 'delai', 'estActive', 'actions'];
  dataSource!: MatTableDataSource<ZoneLivraisonDTO>;
  isLoading = false;
  errorMessage: string | null = null;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(private zoneLivraisonService: ZoneLivraisonService) {}

  ngOnInit(): void {
    this.loadZones();
  }

  loadZones(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.zoneLivraisonService.getAllZones().subscribe({
      next: (zones) => {
        this.zones = zones;
        this.dataSource = new MatTableDataSource(this.zones);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading zones:', error);
        this.errorMessage = 'Impossible de charger les zones de livraison.';
        this.isLoading = false;
      }
    });
  }

  /** Les frais sont en FCFA : 0 signifie livraison incluse. */
  formatFrais(frais: number | undefined): string {
    if (!frais) {
      return 'Incluse';
    }
    return `${new Intl.NumberFormat('fr-FR').format(frais)} FCFA`;
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  deleteZone(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette zone ?')) {
      this.zoneLivraisonService.deleteZone(id).subscribe({
        next: () => {
          this.loadZones();
        },
        error: (error) => {
          console.error('Error deleting zone:', error);
        }
      });
    }
  }
}