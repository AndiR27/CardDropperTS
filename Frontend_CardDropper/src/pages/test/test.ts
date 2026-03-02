import { Component, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { AuthService } from '../../app/core/auth/auth.service';
import { CardService } from '../../app/services/card.service';
import { MeService } from '../../app/services/me.service';
import { AdminService } from '../../app/services/admin.service';
import { ErrorDisplay } from '../../app/components/error/error-display';
import type { Card, User, PackTemplate } from '../../app/models';

@Component({
  selector: 'app-test',
  imports: [JsonPipe, ErrorDisplay],
  templateUrl: './test.html',
  styleUrl: './test.scss',
})
export class TestPage {
  protected readonly auth = inject(AuthService);
  private readonly cardService = inject(CardService);
  private readonly meService = inject(MeService);
  private readonly adminService = inject(AdminService);

  protected readonly cards = signal<Card[]>([]);
  protected readonly myCards = signal<Card[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly templates = signal<PackTemplate[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly lastTest = signal<string>('');

  login(): void {
    this.auth.login();
  }

  logout(): void {
    this.auth.logout();
  }

  loadAllCards(): void {
    this.clearState();
    this.lastTest.set('GET /cards (public)');
    this.loading.set(true);
    this.cardService.getAll().subscribe({
      next: (data) => { this.cards.set(data); this.loading.set(false); },
      error: (err) => this.handleError(err),
    });
  }

  loadMyCards(): void {
    this.clearState();
    this.lastTest.set('GET /me/cards/owned (authenticated)');
    this.loading.set(true);
    this.meService.getCardsOwned().subscribe({
      next: (data) => { this.myCards.set(data); this.loading.set(false); },
      error: (err) => this.handleError(err),
    });
  }

  loadUsers(): void {
    this.clearState();
    this.lastTest.set('GET /admin/users (admin)');
    this.loading.set(true);
    this.adminService.getUsers().subscribe({
      next: (data) => { this.users.set(data); this.loading.set(false); },
      error: (err) => this.handleError(err),
    });
  }

  loadPackTemplates(): void {
    this.clearState();
    this.lastTest.set('GET /admin/pack-templates (admin)');
    this.loading.set(true);
    this.adminService.getPackTemplates().subscribe({
      next: (data) => { this.templates.set(data); this.loading.set(false); },
      error: (err) => this.handleError(err),
    });
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  private clearState(): void {
    this.error.set(null);
    this.cards.set([]);
    this.myCards.set([]);
    this.users.set([]);
    this.templates.set([]);
  }

  private handleError(err: { status: number; message: string }): void {
    this.error.set(`Erreur ${err.status}: ${err.message}`);
    this.loading.set(false);
  }
}
