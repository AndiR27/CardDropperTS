import { Component, computed, inject, input, output, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { Router } from '@angular/router';
import { Card, Rarity } from '../../../app/models';
import { CardService } from '../../../app/services/card.service';
import { MeService } from '../../../app/services/me.service';

type MergeRarity = Rarity.COMMON | Rarity.RARE;
type FilterRarity = MergeRarity | Rarity.EPIC;

const NEXT_RARITY: Record<MergeRarity, Rarity> = {
  [Rarity.COMMON]: Rarity.RARE,
  [Rarity.RARE]:   Rarity.EPIC,
};

const MERGE_REQUIRED: Record<MergeRarity, number> = {
  [Rarity.COMMON]: 5,
  [Rarity.RARE]:   5,
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

  protected readonly rarityFilter = signal<FilterRarity>(Rarity.COMMON);
  protected readonly selectedIndices = signal<Set<number>>(new Set());
  protected readonly merging = signal(false);
  protected readonly mergeResult = signal<Card | null>(null);
  protected readonly showConfirm = signal(false);
  protected readonly mergeError = signal<string | null>(null);
  protected readonly recycleSuccess = signal(false);

  protected readonly isRecycleMode = computed(() => this.rarityFilter() === Rarity.EPIC);

  protected readonly rarities: { value: FilterRarity; label: string }[] = [
    { value: Rarity.COMMON, label: 'Commune' },
    { value: Rarity.RARE,   label: 'Rare' },
    // { value: Rarity.EPIC,   label: 'Épique' },  // temporarily disabled
  ];

  protected readonly filteredCards = computed(() => {
    return this.cards().filter(c => c.rarity === this.rarityFilter());
  });

  protected readonly requiredCount = computed(() =>
    this.isRecycleMode() ? 1 : MERGE_REQUIRED[this.rarityFilter() as MergeRarity]
  );

  protected readonly selectionCount = computed(() => this.selectedIndices().size);
  protected readonly canAct = computed(() => this.selectedIndices().size === this.requiredCount());

  protected readonly selectedCards = computed(() => {
    const indices = this.selectedIndices();
    const cards = this.filteredCards();
    return Array.from(indices).map(i => cards[i]).filter(Boolean);
  });

  protected readonly resultRarity = computed(() => {
    if (this.isRecycleMode()) return null;
    return NEXT_RARITY[this.rarityFilter() as MergeRarity];
  });

  setRarity(r: FilterRarity): void {
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
    if (!this.canAct()) return;
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

  confirmAction(): void {
    if (this.isRecycleMode()) {
      this.confirmRecycle();
    } else {
      this.confirmMerge();
    }
  }

  private confirmMerge(): void {
    if (!this.canAct() || this.merging()) return;
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

  private confirmRecycle(): void {
    const card = this.selectedCards()[0];
    if (!card || card.id === null || this.merging()) return;
    this.merging.set(true);
    this.mergeError.set(null);
    this.meService.recycleCard(card.id).subscribe({
      next: () => {
        this.merging.set(false);
        this.showConfirm.set(false);
        this.selectedIndices.set(new Set());
        this.recycleSuccess.set(true);
      },
      error: (err) => {
        this.merging.set(false);
        this.mergeError.set(err?.error?.detail ?? 'Erreur lors du recyclage.');
      },
    });
  }

  closeRecycleSuccess(): void {
    this.recycleSuccess.set(false);
    this.router.navigateByUrl('/', { skipLocationChange: true }).then(() => {
      this.router.navigate(['/my-cards']);
    });
  }

  onRecycleSuccessOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('merge-reveal')) {
      this.closeRecycleSuccess();
    }
  }

  closeResult(): void {
    this.mergeResult.set(null);
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
