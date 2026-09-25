import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommandeDTO } from '../dto/commande.dto';

@Injectable({
  providedIn: 'root'
})
export class CommandeService {
  private apiUrl = '/api/admin/commandes';

  constructor(private http: HttpClient) {}

  getAllCommandes(): Observable<CommandeDTO[]> {
    return this.http.get<CommandeDTO[]>(this.apiUrl);
  }

  getCommandeById(id: number): Observable<CommandeDTO> {
    return this.http.get<CommandeDTO>(`${this.apiUrl}/${id}`);
  }

  updateStatut(id: number, statut: string, commentaires?: string): Observable<CommandeDTO> {
    return this.http.patch<CommandeDTO>(`${this.apiUrl}/${id}/statut`, { statut, commentaires });
  }

  deleteCommande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}