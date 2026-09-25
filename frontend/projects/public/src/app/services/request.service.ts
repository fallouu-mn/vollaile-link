import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PublicDemandeAcknowledgementDTO,
  RequestDTO
} from '../dto/request.dto';

@Injectable({
  providedIn: 'root'
})
export class RequestService {

  private apiUrl = '/api/public/demandes';

  constructor(private http: HttpClient) { }

  submitRequest(requestData: RequestDTO): Observable<PublicDemandeAcknowledgementDTO> {
    return this.http.post<PublicDemandeAcknowledgementDTO>(this.apiUrl, requestData);
  }
}