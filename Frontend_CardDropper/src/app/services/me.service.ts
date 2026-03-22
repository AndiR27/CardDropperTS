import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Card } from '../models';
import type { User, LiveFeedEvent, UserPackInventory } from '../models';

@Injectable({ providedIn: 'root' })
export class MeService {

  private readonly api = inject(ApiService);

  // ====== Cards ======

  getCardsOwned(): Observable<Card[]> {
    return this.api.get<Card[]>('/me/cards/owned');
  }

  getCardsCreated(): Observable<Card[]> {
    return this.api.get<Card[]>('/me/cards/created');
  }

  getCardsTargeting(): Observable<Card[]> {
    return this.api.get<Card[]>('/me/cards/targeting');
  }

  createCardWithImage(card: Card, image: File): Observable<Card> {
    const formData = new FormData();
    formData.append('card', new Blob([JSON.stringify(card)], { type: 'application/json' }));
    formData.append('image', image);
    return this.api.postMultipart<Card>('/me/cards/with-image', formData);
  }

  // ====== Trading ======

  tradeCards(myCardId: number, targetUserId: number, theirCardId: number): Observable<void> {
    return this.api.post<void>(
      `/me/cards/trade?myCardId=${myCardId}&targetUserId=${targetUserId}&theirCardId=${theirCardId}`,
      null
    );
  }

  // ====== Merging ======

  mergeCards(cardIds: number[]): Observable<Card> {
    return this.api.post<Card>('/me/cards/merge', cardIds);
  }

  // ====== Use Card ======

  useCard(cardId: number, targetUserId: number): Observable<void> {
    return this.api.post<void>(`/me/cards/use?cardId=${cardId}&targetUserId=${targetUserId}`, null);
  }

  deactivateCard(cardId: number): Observable<void> {
    return this.api.patch<void>(`/me/cards/${cardId}/deactivate`);
  }

  // ====== Live Feed ======

  getLiveFeedToday(): Observable<LiveFeedEvent[]> {
    return this.api.get<LiveFeedEvent[]>('/me/live-feed/today');
  }

  // ====== Joueurs ======

  getAllUsers(): Observable<User[]> {
    return this.api.get<User[]>('/me/users');
  }

  // ====== Packs ======

  getPackInventory(): Observable<UserPackInventory[]> {
    return this.api.get<UserPackInventory[]>('/me/packs');
  }

  openPack(cardIds: number[]): Observable<Card[]> {
    return this.api.post<Card[]>('/me/packs/open', cardIds);
  }

  generatePack(templateId: number): Observable<Card[]> {
    return this.api.post<Card[]>(`/me/packs/generate?templateId=${templateId}`, null);
  }
}
