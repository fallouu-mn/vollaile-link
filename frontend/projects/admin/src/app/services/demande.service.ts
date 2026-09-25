import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DemandeDTO } from '../dto/demande.dto';

@Injectable({
  providedIn: 'root'
})
export class DemandeService {
  private apiUrl = '/api/admin/demandes';

  constructor(private http: HttpClient) {}

  getAllDemandes(): Observable<DemandeDTO[]> {
    return this.http.get<DemandeDTO[]>(this.apiUrl);
  }

  getDemandeById(id: number): Observable<DemandeDTO> {
    return this.http.get<DemandeDTO>(`${this.apiUrl}/${id}`);
  }

  createDemande(demande: DemandeDTO): Observable<DemandeDTO> {
    return this.http.post<DemandeDTO>(this.apiUrl, demande);
  }

  updateDemande(id: number, demande: DemandeDTO): Observable<DemandeDTO> {
    return this.http.put<DemandeDTO>(`${this.apiUrl}/${id}`, demande);
  }

  deleteDemande(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  searchDemandes(searchTerm: string): Observable<DemandeDTO[]> {
    return this.http.get<DemandeDTO[]>(`${this.apiUrl}/search`, { params: { search: searchTerm } });
  }

  updateStatut(id: number, statut: string, commentairesAdmin?: string): Observable<DemandeDTO> {
    return this.http.patch<DemandeDTO>(`${this.apiUrl}/${id}/statut`, { statut, commentairesAdmin });
  }

  convertToCommande(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/convert-to-commande`, {});
  }
}