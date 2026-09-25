import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientDTO } from '../dto/client.dto';

@Injectable({
  providedIn: 'root'
})
export class ClientService {
  private apiUrl = '/api/admin/clients';

  constructor(private http: HttpClient) {}

  getAllClients(): Observable<ClientDTO[]> {
    return this.http.get<ClientDTO[]>(this.apiUrl);
  }

  getClientById(id: number): Observable<ClientDTO> {
    return this.http.get<ClientDTO>(`${this.apiUrl}/${id}`);
  }

  createClient(client: ClientDTO): Observable<ClientDTO> {
    return this.http.post<ClientDTO>(this.apiUrl, client);
  }

  updateClient(id: number, client: ClientDTO): Observable<ClientDTO> {
    return this.http.put<ClientDTO>(`${this.apiUrl}/${id}`, client);
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  searchClients(searchTerm: string): Observable<ClientDTO[]> {
    return this.http.get<ClientDTO[]>(`${this.apiUrl}/search`, { params: { search: searchTerm } });
  }

  telephoneExists(telephone: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/telephone-exists`, { params: { telephone } });
  }
}