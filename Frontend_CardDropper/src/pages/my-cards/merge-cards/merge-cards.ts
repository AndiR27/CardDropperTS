import { Component, computed, inject, input, output, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Card, Rarity } from '../../../app/models';
import { CardService } from '../../../app/services/card.service';
import { MeService } from '../../../app/services/me.service';

type MergeRarity = Rarity.COMMON | Rarity.RARE | Rarity.EPIC;

const NEXT_RARITY: Record<MergeRarity, Rarity> = {
  [Rarity.COMMON]: Rarity.RARE,
  [Rarity.RARE]: Rarity.EPIC,
  [Rarity.EPIC]: Rarity.LEGENDARY,
};

const MERGE_REQUIRED: Record<MergeRarity, number> = {
  [Rarity.COMMON]: 3,
  [Rarity.RARE]: 4,
  [Rarity.EPIC]: 5,
};

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
  private readonly router = inject(Router);

  readonly cards = input.required<Card[]>();
  readonly merged = output<Card>();

  protected readonly rarityFilter = signal<MergeRarity>(Rarity.COMMON);
  protected readonly selectedIndices = signal<Set<number>>(new Set());
  protected readonly merging = signal(false);
  protected readonly mergeResult = signal<Card | null>(null);
  protected readonly showConfirm = signal(false);
  protected readonly mergeError = signal<string | null>(null);

  protected readonly rarities: { value: MergeRarity; label: string }[] = [
    { value: Rarity.COMMON, label: 'Commune' },
    { value: Rarity.RARE, label: 'Rare' },
    { value: Rarity.EPIC, label: 'Épique' },
  ];

  protected readonly filteredCards = computed(() => {
    return this.cards().filter(c => c.rarity === this.rarityFilter());
  });

  protected readonly requiredCount = computed(() => MERGE_REQUIRED[this.rarityFilter()]);
  protected readonly selectionCount = computed(() => this.selectedIndices().size);
  protected readonly canMerge = computed(() => this.selectedIndices().size === this.requiredCount());

  protected readonly selectedCards = computed(() => {
    const indices = this.selectedIndices();
    const cards = this.filteredCards();
    return Array.from(indices).map(i => cards[i]).filter(Boolean);
  });

  protected readonly resultRarity = computed(() => {
    return NEXT_RARITY[this.rarityFilter()];
  });

  setRarity(r: MergeRarity): void {
    this.rarityFilter.set(r);
    this.selectedIndices.set(new Set());
  }

  toggleCard(index: number): void {
    const indices = new Set(this.selectedIndices());
    if (indices.has(index)) {
      indices.delete(index);
    } else if (indices.size < this.requiredCount()) {
      indices.add(index);
    }
    this.selectedIndices.set(indices);
  }

  isSelected(index: number): boolean {
    return this.selectedIndices().has(index);
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  openConfirm(): void {
    if (!this.canMerge()) return;
    this.mergeError.set(null);
    this.showConfirm.set(true);
  }

  closeConfirm(): void {
    this.showConfirm.set(false);
  }

  onConfirmOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('merge-confirm')) {
      this.closeConfirm();
    }
  }

  confirmMerge(): void {
    if (!this.canMerge() || this.merging()) return;
    this.merging.set(true);
    this.mergeError.set(null);
    const cards = this.filteredCards();
    const ids = Array.from(this.selectedIndices())
      .map(i => cards[i].id)
      .filter((id): id is number => id !== null);
    this.meService.mergeCards(ids).subscribe({
      next: (result) => {
        this.merging.set(false);
        this.showConfirm.set(false);
        this.selectedIndices.set(new Set());
        this.mergeResult.set(result);
        this.merged.emit(result);
      },
      error: (err) => {
        this.merging.set(false);
        this.mergeError.set(err?.error?.detail ?? 'Erreur lors de la fusion.');
      },
    });
  }

  closeResult(): void {
    this.mergeResult.set(null);
    // Navigate away then back to force component reload
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
      this.router.navigate(['/my-cards']);
    });
  }

  onResultOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('merge-reveal')) {
      this.closeResult();
    }
  }
}
