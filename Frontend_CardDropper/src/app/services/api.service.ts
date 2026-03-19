import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

/**
 * Service générique pour communiquer avec le backend CardDropper.
 *
 * Utilisation :
 *   - Toutes les requêtes passent par ce service (centralise l'URL de base)
 *   - Le token JWT est ajouté automatiquement par l'interceptor (auth.interceptor.ts)
 *   - Les méthodes retournent des Observable<T> (subscribe dans le composant)
 *
 * Exemple :
 *   this.api.get<UserDto>('/auth/me').subscribe(user => console.log(user));
 *   this.api.post<CardDto>('/me/cards', cardData).subscribe(card => ...);
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /** GET — récupérer des données */
  get<T>(path: string, params?: Record<string, string | number>): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, { params });
  }

  /** POST — créer une ressource */
  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  /** PUT — mettre à jour une ressource */
  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}${path}`, body);
  }

  /** DELETE — supprimer une ressource */
  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}${path}`);
  }

  /** POST multipart — envoyer un formulaire avec fichier */
  postMultipart<T>(path: string, formData: FormData): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, formData);
  }

  /** Construit l'URL complète d'une image de carte à partir de son imageUrl relative */
  cardImageUrl(imageUrl: string): string {
    return `${this.baseUrl}/cards/images/${imageUrl}`;
  }
}
