import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Card, Rarity } from '../models';

@Injectable({ providedIn: 'root' })
export class CardService {

  private readonly api = inject(ApiService);

  getAll(): Observable<Card[]> {
    return this.api.get<Card[]>('/cards');
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

  /** Construit l'URL complète de l'image d'une carte */
  imageUrl(card: Card): string | null {
    return card.imageUrl ? this.api.cardImageUrl(card.imageUrl) : null;
  }
}
