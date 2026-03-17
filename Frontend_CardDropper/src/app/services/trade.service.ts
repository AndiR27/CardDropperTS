import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import type { TradeSession, OnlineUser } from '../models';

@Injectable({ providedIn: 'root' })
export class TradeService {

  private readonly api = inject(ApiService);

  getOnlineUsers(): Observable<OnlineUser[]> {
    return this.api.get<OnlineUser[]>('/trades/online');
  }

  createSession(receiverId: number): Observable<TradeSession> {
    return this.api.post<TradeSession>('/trades', { receiverId });
  }

  joinSession(sessionId: string): Observable<TradeSession> {
    return this.api.post<TradeSession>(`/trades/${sessionId}/join`, null);
  }

  getState(sessionId: string): Observable<TradeSession> {
    return this.api.get<TradeSession>(`/trades/${sessionId}`);
  }

  getActiveSession(): Observable<TradeSession | null> {
    return this.api.get<TradeSession | null>('/trades/active');
  }
}
