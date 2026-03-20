import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe, UpperCasePipe } from '@angular/common';
import { MeService } from '../../app/services/me.service';
import { CardService } from '../../app/services/card.service';
import { AuthService } from '../../app/core/auth/auth.service';
import { Card } from '../../app/models';
import type { User } from '../../app/models';
import { MergeCardsComponent } from './merge-cards/merge-cards';
import { CardListComponent } from './card-list/card-list';

@Component({
  selector: 'app-my-cards',
  standalone: true,
  imports: [LowerCasePipe, UpperCasePipe, MergeCardsComponent, CardListComponent],
  templateUrl: './my-cards.html',
  styleUrl: './my-cards.scss',
})
export class MyCardsPage implements OnInit {
  private readonly meService = inject(MeService);
  private readonly cardService = inject(CardService);
  private readonly authService = inject(AuthService);

  // ── Tabs ──
  protected readonly activeTab = signal<'cards' | 'merge' | 'creations' | 'targets'>('cards');

  // ── Data ──
  protected readonly ownedCards = signal<Card[]>([]);
  protected readonly createdCards = signal<Card[]>([]);
  protected readonly targetingCards = signal<Card[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(false);

  // ── View mode ──
  protected readonly viewMode = signal<'carousel' | 'list'>('carousel');
  protected readonly listSearch = signal('');

  protected readonly countMap = computed(() => {
    const map = new Map<number, number>();
    for (const c of this.ownedCards()) {
      if (c.id !== null) map.set(c.id, (map.get(c.id) ?? 0) + 1);
    }
    return map;
  });

  protected readonly uniqueOwnedCards = computed(() => {
    const seen = new Set<number>();
    return this.ownedCards().filter(c => {
      if (c.id === null || seen.has(c.id)) return false;
      seen.add(c.id);
      return true;
    });
  });

  getCount(card: Card): number {
    return card.id !== null ? (this.countMap().get(card.id) ?? 1) : 1;
  }

  protected readonly filteredOwnedCards = computed(() => {
    const search = this.listSearch().toLowerCase().trim();
    const cards = this.uniqueOwnedCards();
    if (!search) return cards;
    return cards.filter(c =>
      c.name.toLowerCase().includes(search) ||
      c.rarity.toLowerCase().includes(search)
    );
  });

  toggleViewMode(): void {
    this.viewMode.update(m => m === 'carousel' ? 'list' : 'carousel');
  }

  selectCardFromList(card: Card): void {
    const index = this.uniqueOwnedCards().findIndex(c => c.id === card.id);
    if (index >= 0) {
      this.activeIndex.set(index);
      this.viewMode.set('carousel');
    }
  }

  // ── Carousel ──
  protected readonly activeIndex = signal(0);

  protected readonly activeCard = computed(() => {
    const cards = this.uniqueOwnedCards();
    if (cards.length === 0) return null;
    return cards[this.activeIndex()];
  });

  protected readonly canPrev = computed(() => this.activeIndex() > 0);
  protected readonly canNext = computed(() => this.activeIndex() < this.uniqueOwnedCards().length - 1);

  // ── Use card modal ──
  protected readonly showUseModal = signal(false);
  protected readonly selectedTarget = signal<User | null>(null);
  protected readonly useStep = signal<'choose' | 'confirm'>('choose');
  protected readonly userSearch = signal('');
  protected readonly showUseSuccess = signal(false);
  protected readonly usedCardName = signal('');
  protected readonly usedTargetName = signal('');

  protected readonly filteredUsers = computed(() => {
    const search = this.userSearch().toLowerCase().trim();
    const currentUsername = this.authService.username;
    const all = this.users().filter(u => u.username !== currentUsername);
    if (!search) return all;
    return all.filter(u => u.username.toLowerCase().includes(search));
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.loadOwnedCards();
    this.meService.getCardsCreated().subscribe({
      next: (cards) => this.createdCards.set(cards),
    });
    this.meService.getCardsTargeting().subscribe({
      next: (cards) => this.targetingCards.set(cards),
    });
    this.meService.getAllUsers().subscribe({
      next: (data) => this.users.set(data),
    });
  }

  private loadOwnedCards(): void {
    this.meService.getCardsOwned().subscribe({
      next: (cards) => { this.ownedCards.set(cards); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onMerged(): void {
    this.loadOwnedCards();
  }

  // ── Carousel navigation ──
  prev(): void {
    if (this.canPrev()) this.activeIndex.update(i => i - 1);
  }

  next(): void {
    if (this.canNext()) this.activeIndex.update(i => i + 1);
  }

  goTo(index: number): void {
    this.activeIndex.set(index);
  }

  // ── Image helper ──
  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  getTargetName(card: Card): string | null {
    if (!card.targetUserId) return null;
    const user = this.users().find(u => u.id === card.targetUserId);
    return user?.username ?? null;
  }

  // ── Use card flow ──
  openUseModal(): void {
    const card = this.activeCard();
    if (!card) return;

    this.selectedTarget.set(null);
    this.userSearch.set('');

    if (card.targetUserId) {
      const target = this.users().find(u => u.id === card.targetUserId) ?? null;
      this.selectedTarget.set(target);
      this.useStep.set('confirm');
    } else {
      this.useStep.set('choose');
    }

    this.showUseModal.set(true);
  }

  selectUser(user: User): void {
    this.selectedTarget.set(user);
    this.useStep.set('confirm');
  }

  backToChoose(): void {
    this.selectedTarget.set(null);
    this.useStep.set('choose');
  }

  protected readonly useError = signal<string | null>(null);
  protected readonly using = signal(false);

  confirmUse(): void {
    const card = this.activeCard();
    const target = this.selectedTarget();
    if (!card?.id || !target?.id || this.using()) return;

    this.using.set(true);
    this.useError.set(null);

    this.meService.useCard(card.id, target.id).subscribe({
      next: () => {
        this.using.set(false);
        this.showUseModal.set(false);

        // Show success popup
        this.usedCardName.set(card.name);
        this.usedTargetName.set(target.username);
        this.showUseSuccess.set(true);
        setTimeout(() => this.showUseSuccess.set(false), 2500);

        // Remove one copy of the card; if more copies remain the badge just decrements
        this.ownedCards.update(cards => {
          const idx = cards.findIndex(c => c.id === card.id);
          return idx >= 0 ? [...cards.slice(0, idx), ...cards.slice(idx + 1)] : cards;
        });
        if (this.activeIndex() >= this.uniqueOwnedCards().length) {
          this.activeIndex.set(Math.max(0, this.uniqueOwnedCards().length - 1));
        }
      },
      error: (err) => {
        this.using.set(false);
        this.useError.set(err?.error?.detail ?? 'Erreur lors de l\'utilisation de la carte.');
      },
    });
  }

  closeUseModal(): void {
    this.showUseModal.set(false);
  }

  onModalOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('use-modal')) {
      this.closeUseModal();
    }
  }
}
