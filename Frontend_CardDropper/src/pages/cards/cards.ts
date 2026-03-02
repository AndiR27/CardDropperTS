import { Component, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { AuthService } from '../../app/core/auth/auth.service';
import { CardService } from '../../app/services/card.service';
import type { Card } from '../../app/models';

@Component({
  selector: 'app-cards',
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class CardsPage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly cardService = inject(CardService);

  protected readonly cards = signal<Card[]>([]);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.loadCards();
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  private loadCards(): void {
    this.loading.set(true);
    this.cardService.getAll().subscribe({
      next: (data) => { this.cards.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
