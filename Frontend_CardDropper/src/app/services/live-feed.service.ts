import { Injectable, inject, OnDestroy, signal } from '@angular/core';
import { environment } from '../environments/environment';
import { AuthService } from '../core/auth/auth.service';
import { MeService } from './me.service';
import type { LiveFeedEvent } from '../models';

@Injectable({ providedIn: 'root' })
export class LiveFeedService implements OnDestroy {

  private readonly auth = inject(AuthService);
  private readonly meService = inject(MeService);
  private abortController: AbortController | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  readonly events = signal<LiveFeedEvent[]>([]);

  connect(): void {
    if (this.abortController) return;

    // Load today's events first
    this.meService.getLiveFeedToday().subscribe({
      next: (events) => this.events.set(events),
    });

    // Connect SSE via fetch (supports Authorization header)
    this.connectStream();
  }

  private async connectStream(): Promise<void> {
    this.abortController = new AbortController();
    const url = `${environment.apiUrl}/me/live-feed/stream`;

    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${this.auth.accessToken}`,
          'Accept': 'text/event-stream',
        },
        signal: this.abortController.signal,
      });

      if (!response.ok || !response.body) return;

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        let eventName = '';
        let dataLines: string[] = [];

        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim());
          } else if (line === '') {
            // End of SSE message
            if (eventName === 'use-card' && dataLines.length > 0) {
              try {
                const event: LiveFeedEvent = JSON.parse(dataLines.join('\n'));
                this.events.update(list => [event, ...list]);
              } catch { /* ignore parse errors */ }
            }
            eventName = '';
            dataLines = [];
          }
        }
      }
    } catch (e: unknown) {
      if (e instanceof DOMException && e.name === 'AbortError') return;
    }

    // Reconnect after 5 seconds if not explicitly disconnected
    if (this.abortController) {
      this.abortController = null;
      this.reconnectTimer = setTimeout(() => this.connectStream(), 5000);
    }
  }

  disconnect(): void {
    this.abortController?.abort();
    this.abortController = null;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
