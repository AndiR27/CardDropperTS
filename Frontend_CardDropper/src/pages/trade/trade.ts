import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TradeService } from '../../app/services/trade.service';
import { WebSocketService } from '../../app/services/websocket.service';
import { AuthService } from '../../app/core/auth/auth.service';
import type { OnlineUser, TradeSession } from '../../app/models';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-trade',
  standalone: true,
  templateUrl: './trade.html',
  styleUrl: './trade.scss',
})
export class TradePage implements OnInit, OnDestroy {

  private readonly tradeService = inject(TradeService);
  private readonly ws = inject(WebSocketService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private subs: Subscription[] = [];

  protected readonly onlineUsers = signal<OnlineUser[]>([]);
  protected readonly pendingInvite = signal<TradeSession | null>(null);
  protected readonly currentUsername = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly infoBoardOpen = signal(false);

  ngOnInit(): void {
    this.currentUsername.set(this.auth.username);

    // Load initial online users via HTTP
    this.tradeService.getOnlineUsers().subscribe({
      next: users => this.onlineUsers.set(users),
      error: () => this.error.set('Impossible de charger les joueurs en ligne'),
    });

    // Check if already in an active session
    this.tradeService.getActiveSession().subscribe({
      next: session => {
        if (session) {
          this.router.navigate(['/trade/session', session.id]);
        }
      },
    });

    // Connect WebSocket
    this.ws.connect();

    const connSub = this.ws.connected$.subscribe(connected => {
      if (connected) {
        // Subscribe to presence updates
        this.ws.subscribe<OnlineUser[]>('/topic/trade/presence', users => {
          this.onlineUsers.set(users);
        });

        // Subscribe to personal trade invites
        this.ws.subscribe<TradeSession>('/user/queue/trade-invites', invite => {
          this.pendingInvite.set(invite);
        });
      }
    });
    this.subs.push(connSub);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.ws.disconnect();
  }

  inviteUser(userId: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.tradeService.createSession(userId).subscribe({
      next: session => {
        this.loading.set(false);
        this.router.navigate(['/trade/session', session.id]);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Impossible de creer la session d\'echange');
      },
    });
  }

  acceptInvite(): void {
    const invite = this.pendingInvite();
    if (!invite) return;

    this.loading.set(true);
    this.tradeService.joinSession(invite.id).subscribe({
      next: session => {
        this.loading.set(false);
        this.pendingInvite.set(null);
        this.router.navigate(['/trade/session', session.id]);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'Impossible de rejoindre la session');
      },
    });
  }

  declineInvite(): void {
    this.pendingInvite.set(null);
  }
}
