import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PageDTO {
  id: number;
  title: string;
  slug: string;
  content: string;
  metaTitle?: string;
  metaDescription?: string;
  ogImage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PageService {

  private apiUrl = '/api/public/pages';

  constructor(private http: HttpClient) { }

  getPageBySlug(slug: string): Observable<PageDTO> {
    return this.http.get<PageDTO>(`${this.apiUrl}/${slug}`);
  }
}