import { Injectable, inject, NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../environments/environment';
import { AuthService } from '../core/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class WebSocketService {

  private readonly auth = inject(AuthService);
  private readonly zone = inject(NgZone);
  private client: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  readonly connected$ = new Subject<boolean>();

  connect(): void {
    if (this.client?.connected) return;

    const token = this.auth.accessToken;
    if (!token) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}${environment.apiUrl}/ws`;

    this.client = new Client({
      brokerURL: wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => this.zone.run(() => this.connected$.next(true)),
      onDisconnect: () => this.zone.run(() => this.connected$.next(false)),
      onStompError: (frame) => console.error('STOMP error:', frame.headers['message']),
    });

    this.client.activate();
  }

  disconnect(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
    this.client?.deactivate();
    this.client = null;
  }

  subscribe<T>(destination: string, callback: (body: T) => void): StompSubscription | null {
    if (!this.client?.connected) return null;

    const sub = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body) as T;
        if (parsed == null || typeof parsed !== 'object') return;
        this.zone.run(() => callback(parsed));
      } catch {
        // Ignore malformed messages from server
      }
    });

    this.subscriptions.push(sub);
    return sub;
  }

  send(destination: string, body: unknown = {}): void {
    this.client?.publish({ destination, body: JSON.stringify(body) });
  }
}
