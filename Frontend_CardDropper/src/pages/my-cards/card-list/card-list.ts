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

  protected readonly filteredCards = computed(() => {
    const q = this.search().toLowerCase().trim();
    const all = this.cards();
    if (!q) return all;
    return all.filter(c => c.name.toLowerCase().includes(q));
  });

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
