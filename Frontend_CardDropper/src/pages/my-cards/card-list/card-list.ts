import { Component, computed, inject, input, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { Card } from '../../../app/models';
import { CardService } from '../../../app/services/card.service';

@Component({
  selector: 'app-card-list',
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: './card-list.html',
  styleUrl: './card-list.scss',
})
export class CardListComponent {
  private readonly cardService = inject(CardService);

  readonly cards = input.required<Card[]>();
  readonly title = input.required<string>();

  protected readonly search = signal('');
  protected readonly hoveredCard = signal<Card | null>(null);

  protected readonly countMap = computed(() => {
    const map = new Map<number, number>();
    for (const c of this.cards()) {
      if (c.id !== null) map.set(c.id, (map.get(c.id) ?? 0) + 1);
    }
    return map;
  });

  protected readonly filteredCards = computed(() => {
    const q = this.search().toLowerCase().trim();
    const seen = new Set<number>();
    const unique = this.cards().filter(c => {
      if (c.id === null || seen.has(c.id)) return false;
      seen.add(c.id);
      return true;
    });
    if (!q) return unique;
    return unique.filter(c => c.name.toLowerCase().includes(q));
  });

  getCount(card: Card): number {
    return card.id !== null ? (this.countMap().get(card.id) ?? 1) : 1;
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  onRowEnter(card: Card, event: MouseEvent): void {
    this.hoveredCard.set(card);
  }

  onRowLeave(): void {
    this.hoveredCard.set(null);
  }
}
