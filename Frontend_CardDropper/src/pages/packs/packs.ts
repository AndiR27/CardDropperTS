import { Component, inject, signal, computed } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { MeService } from '../../app/services/me.service';
import { CardService } from '../../app/services/card.service';
import type { Card } from '../../app/models';

@Component({
  selector: 'app-packs',
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: './packs.html',
  styleUrl: './packs.scss',
})
export class PacksPage {
  private readonly meService = inject(MeService);
  private readonly cardService = inject(CardService);

  protected readonly opening = signal(false);
  protected readonly revealedCards = signal<Card[]>([]);
  protected readonly flippedSet = signal<Set<number>>(new Set());
  protected readonly error = signal<string | null>(null);
  protected readonly phase = signal<'idle' | 'opening' | 'burst' | 'reveal'>('idle');
  protected readonly focusedCard = signal<Card | null>(null);

  protected readonly allFlipped = computed(() => {
    return this.revealedCards().length > 0
      && this.flippedSet().size === this.revealedCards().length;
  });

  openPack(): void {
    if (this.opening()) return;

    this.opening.set(true);
    this.error.set(null);
    this.phase.set('opening');

    this.meService.generatePack(1).subscribe({
      next: (cards) => {
        this.opening.set(false);
        this.revealedCards.set(cards);
        this.flippedSet.set(new Set());
        this.focusedCard.set(null);

        // Burst then reveal
        this.phase.set('burst');
        setTimeout(() => this.phase.set('reveal'), 1000);
      },
      error: (err) => {
        this.opening.set(false);
        this.phase.set('idle');
        this.error.set(err?.error?.detail ?? 'Erreur lors de l\'ouverture du pack.');
      },
    });
  }

  flipCard(index: number): void {
    const cards = this.revealedCards();
    if (index < 0 || index >= cards.length) return;

    const set = new Set(this.flippedSet());
    if (set.has(index)) {
      this.focusedCard.set(cards[index]);
      return;
    }
    set.add(index);
    this.flippedSet.set(set);
  }

  isFlipped(index: number): boolean {
    return this.flippedSet().has(index);
  }

  closeFocus(): void {
    this.focusedCard.set(null);
  }

  onFocusOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('focus-overlay')) {
      this.closeFocus();
    }
  }

  backToIdle(): void {
    this.phase.set('idle');
    this.revealedCards.set([]);
    this.focusedCard.set(null);
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }
}
