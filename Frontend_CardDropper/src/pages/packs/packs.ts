import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { MeService } from '../../app/services/me.service';
import { CardService } from '../../app/services/card.service';
import type { Card, UserPackInventory } from '../../app/models';

@Component({
  selector: 'app-packs',
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: './packs.html',
  styleUrl: './packs.scss',
})
export class PacksPage implements OnInit {
  private readonly meService = inject(MeService);
  private readonly cardService = inject(CardService);

  // ── Inventory ──
  protected readonly inventory = signal<UserPackInventory[]>([]);
  protected readonly selectedPack = signal<UserPackInventory | null>(null);

  protected readonly opening = signal(false);
  protected readonly revealedCards = signal<Card[]>([]);
  protected readonly flippedSet = signal<Set<number>>(new Set());
  protected readonly error = signal<string | null>(null);
  protected readonly phase = signal<'idle' | 'opening' | 'burst' | 'reveal'>('idle');
  protected readonly focusedCard = signal<Card | null>(null);
  protected readonly infoBoardOpen = signal(false);

  protected readonly allFlipped = computed(() => {
    return this.revealedCards().length > 0
      && this.flippedSet().size === this.revealedCards().length;
  });

  ngOnInit(): void {
    this.loadInventory();
  }

  private loadInventory(): void {
    this.meService.getPackInventory().subscribe({
      next: (inv) => this.inventory.set(inv),
    });
  }

  selectPack(pack: UserPackInventory): void {
    this.selectedPack.set(pack);
  }

  openPack(): void {
    const pack = this.selectedPack();
    if (this.opening() || !pack) return;

    this.opening.set(true);
    this.error.set(null);
    this.phase.set('opening');

    this.meService.generatePack(pack.templateId).subscribe({
      next: (cards) => {
        this.opening.set(false);
        this.revealedCards.set(cards);
        this.flippedSet.set(new Set());
        this.focusedCard.set(null);

        // Update local inventory
        this.inventory.update(inv =>
          inv.map(i => i.templateId === pack.templateId
            ? { ...i, quantity: i.quantity - 1 }
            : i
          ).filter(i => i.quantity > 0)
        );

        // Burst then reveal
        this.phase.set('burst');
        setTimeout(() => this.phase.set('reveal'), 1000);
      },
      error: (err) => {
        this.opening.set(false);
        this.phase.set('idle');
        this.error.set(err?.error?.detail ?? err?.error?.message ?? 'Erreur lors de l\'ouverture du pack.');
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
    this.selectedPack.set(null);
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }
}
