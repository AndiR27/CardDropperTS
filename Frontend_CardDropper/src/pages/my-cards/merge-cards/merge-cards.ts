import { Component, computed, inject, input, output, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { Card, Rarity } from '../../../app/models';
import { CardService } from '../../../app/services/card.service';
import { MeService } from '../../../app/services/me.service';

type MergeRarity = Rarity.COMMON | Rarity.RARE | Rarity.EPIC;

@Component({
  selector: 'app-merge-cards',
  standalone: true,
  imports: [LowerCasePipe],
  templateUrl: './merge-cards.html',
  styleUrl: './merge-cards.scss',
})
export class MergeCardsComponent {
  private readonly cardService = inject(CardService);
  private readonly meService = inject(MeService);

  readonly cards = input.required<Card[]>();
  readonly merged = output<Card>();

  protected readonly rarityFilter = signal<MergeRarity>(Rarity.COMMON);
  protected readonly selectedIds = signal<Set<number>>(new Set());
  protected readonly merging = signal(false);
  protected readonly mergeResult = signal<Card | null>(null);

  protected readonly rarities: { value: MergeRarity; label: string }[] = [
    { value: Rarity.COMMON, label: 'Commune' },
    { value: Rarity.RARE, label: 'Rare' },
    { value: Rarity.EPIC, label: 'Épique' },
  ];

  protected readonly filteredCards = computed(() => {
    return this.cards().filter(c => c.rarity === this.rarityFilter());
  });

  protected readonly selectionCount = computed(() => this.selectedIds().size);
  protected readonly canMerge = computed(() => this.selectedIds().size === 3);

  setRarity(r: MergeRarity): void {
    this.rarityFilter.set(r);
    this.selectedIds.set(new Set());
  }

  toggleCard(card: Card): void {
    const ids = new Set(this.selectedIds());
    if (card.id === null) return;
    if (ids.has(card.id)) {
      ids.delete(card.id);
    } else if (ids.size < 3) {
      ids.add(card.id);
    }
    this.selectedIds.set(ids);
  }

  isSelected(card: Card): boolean {
    return card.id !== null && this.selectedIds().has(card.id);
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  merge(): void {
    if (!this.canMerge() || this.merging()) return;
    this.merging.set(true);
    const ids = Array.from(this.selectedIds());
    this.meService.mergeCards(ids).subscribe({
      next: (result) => {
        this.merging.set(false);
        this.selectedIds.set(new Set());
        this.mergeResult.set(result);
        this.merged.emit(result);
        setTimeout(() => this.mergeResult.set(null), 3000);
      },
      error: () => this.merging.set(false),
    });
  }
}
