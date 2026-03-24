import { Component, OnInit, OnDestroy, HostListener, inject, signal, computed } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TradeService } from '../../../app/services/trade.service';
import { WebSocketService } from '../../../app/services/websocket.service';
import { MeService } from '../../../app/services/me.service';
import { ApiService } from '../../../app/services/api.service';
import { AuthService } from '../../../app/core/auth/auth.service';
import type { TradeSession, Card } from '../../../app/models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-trade-card',
  standalone: true,
  imports: [LowerCasePipe, FormsModule],
  templateUrl: './trade-card.html',
  styleUrl: './trade-card.scss',
})
export class TradeCardPage implements OnInit, OnDestroy {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tradeService = inject(TradeService);
  private readonly ws = inject(WebSocketService);
  private readonly meService = inject(MeService);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private subs: Subscription[] = [];

  protected readonly session = signal<TradeSession | null>(null);
  protected readonly myCards = signal<Card[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly currentUsername = signal<string | null>(null);
  protected readonly phase = signal<'live' | 'trading' | 'done'>('live');
  protected readonly pickerSearch = signal('');

  private readonly countMap = computed(() => {
    const map = new Map<number, number>();
    for (const c of this.myCards()) {
      if (c.id !== null) map.set(c.id!, (map.get(c.id!) ?? 0) + 1);
    }
    return map;
  });

  private readonly uniqueMyCards = computed(() => {
    const seen = new Set<number>();
    return this.myCards().filter(c => {
      if (c.id === null || seen.has(c.id!)) return false;
      seen.add(c.id!);
      return true;
    });
  });

  protected readonly filteredMyCards = computed(() => {
    const search = this.pickerSearch().toLowerCase().trim();
    const cards = this.uniqueMyCards();
    if (!search) return cards;
    return cards.filter(c => c.name.toLowerCase().includes(search));
  });

  getCount(card: Card): number {
    return card.id !== null ? (this.countMap().get(card.id!) ?? 1) : 1;
  }

  protected readonly isInitiator = computed(() => {
    const s = this.session();
    return s ? s.initiatorUsername === this.currentUsername() : false;
  });

  protected readonly myCard = computed(() => {
    const s = this.session();
    if (!s) return null;
    return this.isInitiator() ? s.initiatorCard : s.receiverCard;
  });

  protected readonly theirCard = computed(() => {
    const s = this.session();
    if (!s) return null;
    return this.isInitiator() ? s.receiverCard : s.initiatorCard;
  });

  protected readonly myLocked = computed(() => {
    const s = this.session();
    if (!s) return false;
    return this.isInitiator() ? s.initiatorLocked : s.receiverLocked;
  });

  protected readonly theirLocked = computed(() => {
    const s = this.session();
    if (!s) return false;
    return this.isInitiator() ? s.receiverLocked : s.initiatorLocked;
  });

  protected readonly opponentName = computed(() => {
    const s = this.session();
    if (!s) return '';
    return this.isInitiator() ? s.receiverUsername : s.initiatorUsername;
  });

  protected readonly isTerminal = computed(() => {
    const s = this.session();
    return s?.status === 'COMPLETED' || s?.status === 'CANCELLED';
  });

  protected readonly rarityMismatch = computed(() => {
    const mine = this.myCard();
    const theirs = this.theirCard();
    if (!mine || !theirs) return false;
    return mine.rarity !== theirs.rarity;
  });

  protected readonly receivedCard = computed(() => {
    const s = this.session();
    if (!s || s.status !== 'COMPLETED') return null;
    return this.isInitiator() ? s.receiverCard : s.initiatorCard;
  });

  protected readonly givenCard = computed(() => {
    const s = this.session();
    if (!s || s.status !== 'COMPLETED') return null;
    return this.isInitiator() ? s.initiatorCard : s.receiverCard;
  });

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasActiveSession()) {
      event.preventDefault();
    }
  }

  hasActiveSession(): boolean {
    const s = this.session();
    if (!s) return false;
    return s.status === 'PENDING' || s.status === 'ACTIVE' || s.status === 'LOCKED';
  }

  ngOnInit(): void {
    this.currentUsername.set(this.auth.username);
    const sessionId = this.route.snapshot.paramMap.get('id')!;

    // Load user's cards for selection
    this.meService.getCardsOwned().subscribe({
      next: cards => this.myCards.set(cards),
    });

    // Load initial session state
    this.tradeService.getState(sessionId).subscribe({
      next: session => this.session.set(session),
      error: () => this.error.set('Session introuvable'),
    });

    // Connect WebSocket and subscribe to session updates
    this.ws.connect();
    const connSub = this.ws.connected$.subscribe(connected => {
      if (connected) {
        this.ws.subscribe<TradeSession>(`/topic/trade/${sessionId}`, session => {
          if (session.status === 'COMPLETED' && this.phase() === 'live') {
            this.phase.set('trading');
            this.session.set(session);
            setTimeout(() => this.phase.set('done'), 2000);
          } else {
            this.session.set(session);
          }
        });

        this.ws.subscribe<{ error: string }>('/user/queue/errors', msg => {
          this.error.set(msg.error);
        });
      }
    });
    this.subs.push(connSub);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.ws.disconnect();
  }

  selectCard(cardId: number): void {
    const s = this.session();
    if (!s || this.isTerminal()) return;
    this.error.set(null);
    this.ws.send(`/app/trade/${s.id}/select`, { cardId });
  }

  lockCard(): void {
    const s = this.session();
    if (!s || this.isTerminal()) return;
    this.error.set(null);
    this.ws.send(`/app/trade/${s.id}/lock`);
  }

  cancelTrade(): void {
    const s = this.session();
    if (!s || this.isTerminal()) return;
    this.error.set(null);
    this.ws.send(`/app/trade/${s.id}/cancel`);
  }

  backToLobby(): void {
    this.router.navigate(['/trade']);
  }

  goToMyCards(): void {
    this.router.navigate(['/my-cards']);
  }

  imageUrl(card: Card): string {
    return card.imageUrl ? this.api.cardImageUrl(card.imageUrl) : '';
  }
}
