import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ZoneLivraisonDTO } from '../dto/zone-livraison.dto';

@Injectable({
  providedIn: 'root'
})
export class ZoneLivraisonService {
  private apiUrl = '/api/admin/zones-livraison';

  constructor(private http: HttpClient) {}

  getAllZones(): Observable<ZoneLivraisonDTO[]> {
    return this.http.get<ZoneLivraisonDTO[]>(this.apiUrl);
  }

  getZoneById(id: number): Observable<ZoneLivraisonDTO> {
    return this.http.get<ZoneLivraisonDTO>(`${this.apiUrl}/${id}`);
  }

  createZone(dto: ZoneLivraisonDTO): Observable<ZoneLivraisonDTO> {
    return this.http.post<ZoneLivraisonDTO>(this.apiUrl, dto);
  }

  updateZone(id: number, dto: ZoneLivraisonDTO): Observable<ZoneLivraisonDTO> {
    return this.http.put<ZoneLivraisonDTO>(`${this.apiUrl}/${id}`, dto);
  }

  deleteZone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
