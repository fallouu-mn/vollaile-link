import { Component, OnInit, ViewChild } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ClientService } from '../../../services/client.service';
import { ClientDTO } from '../../../dto/client.dto';
import { ExportService } from '../../../services/export.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';

@Component({
  selector: 'app-client-list',
  standalone: false,
  templateUrl: './client-list.component.html',
  styleUrls: ['./client-list.component.scss']
})
export class ClientListComponent implements OnInit {
  clients: ClientDTO[] = [];
  displayedColumns: string[] = ['id', 'nom', 'prenom', 'telephone', 'email', 'ville', 'region', 'professionnel', 'isActive', 'actions'];
  dataSource!: MatTableDataSource<ClientDTO>;
  isLoading = false;
  isExporting = false;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private clientService: ClientService,
    private exportService: ExportService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadClients();
  }

  loadClients(): void {
    this.isLoading = true;
    this.clientService.getAllClients().subscribe({
      next: (clients) => {
        this.clients = clients;
        this.dataSource = new MatTableDataSource(this.clients);
        this.dataSource.paginator = this.paginator;
        this.dataSource.sort = this.sort;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading clients:', error);
        this.isLoading = false;
      }
    });
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  /**
   * Export CSV. Contient des données personnelles : l'export est journalisé
   * côté serveur (CDC §2.5), et l'administrateur en est informé.
   */
  exportCsv(): void {
    this.isExporting = true;
    this.exportService.download('clients').subscribe({
      next: () => {
        this.isExporting = false;
        this.snackBar.open('Export téléchargé et journalisé', 'Fermer', { duration: 4000 });
      },
      error: (error) => {
        console.error('Error exporting clients:', error);
        this.isExporting = false;
        this.snackBar.open('Erreur lors de l\'export', 'Fermer', { duration: 5000 });
      }
    });
  }

  deleteClient(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce client ?')) {
      this.clientService.deleteClient(id).subscribe({
        next: () => {
          this.loadClients();
        },
        error: (error) => {
          console.error('Error deleting client:', error);
        }
      });
    }
  }
}