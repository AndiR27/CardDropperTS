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
  protected readonly selectedIds = signal<Set<number>>(new Set());
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

  protected readonly selectionCount = computed(() => this.selectedIds().size);
  protected readonly canMerge = computed(() => this.selectedIds().size === 3);

  protected readonly selectedCards = computed(() => {
    const ids = this.selectedIds();
    return this.cards().filter(c => c.id !== null && ids.has(c.id));
  });

  protected readonly resultRarity = computed(() => {
    return NEXT_RARITY[this.rarityFilter()];
  });

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
    const ids = Array.from(this.selectedIds());
    this.meService.mergeCards(ids).subscribe({
      next: (result) => {
        this.merging.set(false);
        this.showConfirm.set(false);
        this.selectedIds.set(new Set());
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
