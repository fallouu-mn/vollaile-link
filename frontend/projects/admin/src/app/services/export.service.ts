import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export type ExportType = 'clients' | 'commandes' | 'stock' | 'reservations' | 'audit';

/**
 * Téléchargement d'exports CSV.
 *
 * Le backend force `Content-Disposition: attachment`, mais on construit
 * aussi un nom de fichier côté client : sans cela, le navigateur nommerait
 * le fichier d'après l'URL, donc « clients » sans extension.
 */
@Injectable({
  providedIn: 'root'
})
export class ExportService {
  private apiUrl = '/api/admin/exports';

  constructor(private http: HttpClient) {}

  /**
   * Récupère un export et déclenche le téléchargement.
   * Chaque export est tracé côté serveur (CDC §2.5).
   */
  download(type: ExportType, params: { [key: string]: string } = {}): Observable<void> {
    return this.http
      .get(`${this.apiUrl}/${type}`, {
        params,
        responseType: 'blob'
      })
      .pipe(
        map((blob) => {
          this.saveBlob(blob, this.buildFilename(type, params));
          return void 0;
        })
      );
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // Libère la mémoire du navigateur : sans cela, chaque export fuit.
    window.URL.revokeObjectURL(url);
  }

  private buildFilename(type: string, params: { [key: string]: string }): string {
    const now = new Date();
    const stamp = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
      '_',
      String(now.getHours()).padStart(2, '0'),
      String(now.getMinutes()).padStart(2, '0')
    ].join('');

    const suffixe = params['statut'] ? `_${params['statut'].toLowerCase()}` : '';
    return `vollailelink_${type}${suffixe}_${stamp}.csv`;
  }
}
