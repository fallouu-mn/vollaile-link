import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FAQDTO {
  id: number;
  question: string;
  answer: string;
  displayOrder: number;
}

@Injectable({
  providedIn: 'root'
})
export class FAQService {

  private apiUrl = '/api/public/faqs';

  constructor(private http: HttpClient) { }

  getAllFAQs(): Observable<FAQDTO[]> {
    return this.http.get<FAQDTO[]>(this.apiUrl);
  }
}