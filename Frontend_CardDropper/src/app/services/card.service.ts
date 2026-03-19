import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Card, PageResponse, Rarity } from '../models';

@Injectable({ providedIn: 'root' })
export class CardService {

  private readonly api = inject(ApiService);

  getAll(): Observable<Card[]> {
    return this.api.get<Card[]>('/cards');
  }

  getPaged(page: number, size: number): Observable<PageResponse<Card>> {
    return this.api.get<PageResponse<Card>>('/cards/paged', { page, size });
  }

  getById(id: number): Observable<Card> {
    return this.api.get<Card>(`/cards/${id}`);
  }

  getByRarity(rarity: Rarity): Observable<Card[]> {
    return this.api.get<Card[]>(`/cards/by-rarity?rarity=${rarity}`);
  }

  create(card: Card): Observable<Card> {
    return this.api.post<Card>('/cards', card);
  }

  update(id: number, card: Card): Observable<Card> {
    return this.api.put<Card>(`/cards/${id}`, card);
  }

  delete(id: number): Observable<void> {
    return this.api.delete<void>(`/cards/${id}`);
  }

  createWithImage(card: Partial<Card>, image: Blob, filename: string): Observable<Card> {
    const formData = new FormData();
    formData.append('card', new Blob([JSON.stringify(card)], { type: 'application/json' }));
    formData.append('image', image, filename);
    return this.api.postMultipart<Card>('/me/cards/with-image', formData);
  }

  /** Construit l'URL complète de l'image d'une carte */
  imageUrl(card: Card): string | null {
    return card.imageUrl ? this.api.cardImageUrl(card.imageUrl) : null;
  }
}
