import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe, UpperCasePipe } from '@angular/common';
import { MeService } from '../../app/services/me.service';
import { CardService } from '../../app/services/card.service';
import { AuthService } from '../../app/core/auth/auth.service';
import { Card, Rarity, RerollCooldown } from '../../app/models';
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

  // ── Data ──
  protected readonly ownedCards = signal<Card[]>([]);
  protected readonly createdCards = signal<Card[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly cooldowns = signal<RerollCooldown[]>([]);
  protected readonly loading = signal(false);

  // ── Filters ──
  protected readonly searchName = signal('');
  protected readonly filterRarity = signal<Rarity | ''>('');
  protected readonly sortBy = signal<'' | 'name-asc' | 'name-desc' | 'rarity-asc' | 'rarity-desc' | 'qty-desc'>('');

  // ── Tabs ──
  protected readonly activeTab = signal<'cards' | 'merge' | 'creations'>('cards');

  // ── Rarity filter options ──
  readonly rarityOptions: { value: Rarity | ''; label: string; image?: string }[] = [
    { value: '',              label: 'Toutes' },
    { value: Rarity.COMMON,   label: 'Commune', image: 'assets/cardsCreation/rarity/common-crystal.png' },
    { value: Rarity.RARE,     label: 'Rare',    image: 'assets/cardsCreation/rarity/rare-crystal.png' },
    { value: Rarity.EPIC,     label: 'Épique',  image: 'assets/cardsCreation/rarity/epic-crystal.png' },
    { value: Rarity.LEGENDARY, label: 'Légendaire', image: 'assets/cardsCreation/rarity/legendary-crystal.png' },
  ];

  private static readonly RARITY_ORDER: Record<string, number> = {
    COMMON: 0, RARE: 1, EPIC: 2, LEGENDARY: 3,
  };

  // ── Counts ──
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

  // ── Filtered + sorted cards ──
  protected readonly filteredCards = computed(() => {
    let cards = this.uniqueOwnedCards();

    const search = this.searchName().toLowerCase().trim();
    if (search) {
      cards = cards.filter(c => c.name.toLowerCase().includes(search));
    }

    const rarity = this.filterRarity();
    if (rarity) {
      cards = cards.filter(c => c.rarity === rarity);
    }

    const sort = this.sortBy();
    if (sort) {
      const countMap = this.countMap();
      cards = [...cards].sort((a, b) => {
        switch (sort) {
          case 'name-asc':    return a.name.localeCompare(b.name);
          case 'name-desc':   return b.name.localeCompare(a.name);
          case 'rarity-asc':  return (MyCardsPage.RARITY_ORDER[a.rarity] ?? 0) - (MyCardsPage.RARITY_ORDER[b.rarity] ?? 0);
          case 'rarity-desc': return (MyCardsPage.RARITY_ORDER[b.rarity] ?? 0) - (MyCardsPage.RARITY_ORDER[a.rarity] ?? 0);
          case 'qty-desc':    return (countMap.get(b.id!) ?? 1) - (countMap.get(a.id!) ?? 1);
          default: return 0;
        }
      });
    }

    return cards;
  });

  protected readonly cardCount = computed(() => this.filteredCards().length);
  protected readonly totalUniqueCount = computed(() => this.uniqueOwnedCards().length);

  // ── Detail modal ──
  protected readonly selectedCard = signal<Card | null>(null);

  // ── Use card flow ──
  protected readonly showUseModal = signal(false);
  protected readonly selectedTarget = signal<User | null>(null);
  protected readonly useStep = signal<'choose' | 'confirm'>('choose');
  protected readonly userSearch = signal('');
  protected readonly showUseSuccess = signal(false);
  protected readonly usedCardName = signal('');
  protected readonly usedTargetName = signal('');
  protected readonly useError = signal<string | null>(null);
  protected readonly using = signal(false);

  protected readonly filteredUsers = computed(() => {
    const search = this.userSearch().toLowerCase().trim();
    const currentUsername = this.authService.username;
    const all = this.users().filter(u => u.username !== currentUsername);
    if (!search) return all;
    return all.filter(u => u.username.toLowerCase().includes(search));
  });

  // ── Reroll flow ──
  protected readonly rerolling = signal(false);
  protected readonly rerollResult = signal<Card | null>(null);
  protected readonly rerollError = signal<string | null>(null);

  // ── Deactivate modal (for creations) ──
  protected readonly showDeactivateModal = signal(false);
  protected readonly deactivateCardData = signal<Card | null>(null);
  protected readonly deactivateStep = signal<'info' | 'confirm'>('info');
  protected readonly deactivating = signal(false);
  protected readonly deactivateError = signal<string | null>(null);


  // ========================================
  //              LIFECYCLE
  // ========================================

  ngOnInit(): void {
    this.loading.set(true);
    this.loadOwnedCards();
    this.meService.getCardsCreated().subscribe({
      next: (cards) => this.createdCards.set(cards),
    });
    this.meService.getAllUsers().subscribe({
      next: (data) => this.users.set(data),
    });
    this.meService.getRerollCooldowns().subscribe({
      next: (cds) => this.cooldowns.set(cds),
    });
  }

  private loadOwnedCards(): void {
    this.meService.getCardsOwned().subscribe({
      next: (cards) => { this.ownedCards.set(cards); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }


  // ========================================
  //              FILTERS
  // ========================================

  selectRarity(value: Rarity | ''): void {
    this.filterRarity.set(this.filterRarity() === value ? '' : value);
  }


  // ========================================
  //            CARD DETAIL MODAL
  // ========================================

  openDetail(card: Card): void {
    this.selectedCard.set(card);
    this.rerollResult.set(null);
    this.rerollError.set(null);
  }

  closeDetail(): void {
    this.selectedCard.set(null);
  }

  onDetailOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('detail-modal')) {
      this.closeDetail();
    }
  }


  // ========================================
  //            USE CARD FLOW
  // ========================================

  openUseModal(): void {
    const card = this.selectedCard();
    if (!card) return;

    this.selectedTarget.set(null);
    this.userSearch.set('');
    this.useError.set(null);

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

  confirmUse(): void {
    const card = this.selectedCard();
    const target = this.selectedTarget();
    if (!card?.id || !target?.id || this.using()) return;

    this.using.set(true);
    this.useError.set(null);

    this.meService.useCard(card.id, target.id).subscribe({
      next: () => {
        this.using.set(false);
        this.showUseModal.set(false);
        this.selectedCard.set(null);

        this.usedCardName.set(card.name);
        this.usedTargetName.set(target.username);
        this.showUseSuccess.set(true);
        setTimeout(() => this.showUseSuccess.set(false), 2500);

        this.ownedCards.update(cards => {
          const idx = cards.findIndex(c => c.id === card.id);
          return idx >= 0 ? [...cards.slice(0, idx), ...cards.slice(idx + 1)] : cards;
        });
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

  onUseOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('use-modal')) {
      this.closeUseModal();
    }
  }


  // ========================================
  //              REROLL
  // ========================================

  isOnCooldown(rarity: Rarity): boolean {
    const cd = this.cooldowns().find(c => c.rarity === rarity);
    if (!cd) return false;
    return new Date(cd.availableAt) > new Date();
  }

  getCooldownEnd(rarity: Rarity): string | null {
    const cd = this.cooldowns().find(c => c.rarity === rarity);
    if (!cd) return null;
    const end = new Date(cd.availableAt);
    if (end <= new Date()) return null;
    return end.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  reroll(): void {
    const card = this.selectedCard();
    if (!card?.id || this.rerolling()) return;

    this.rerolling.set(true);
    this.rerollError.set(null);

    this.meService.rerollCard(card.id).subscribe({
      next: (response) => {
        this.rerolling.set(false);
        this.rerollResult.set(response.receivedCard);

        // Update owned cards: remove sacrificed, add received
        this.ownedCards.update(cards => {
          const idx = cards.findIndex(c => c.id === card.id);
          const updated = idx >= 0 ? [...cards.slice(0, idx), ...cards.slice(idx + 1)] : [...cards];
          updated.push(response.receivedCard);
          return updated;
        });

        // Refresh cooldowns
        this.meService.getRerollCooldowns().subscribe({
          next: (cds) => this.cooldowns.set(cds),
        });
      },
      error: (err) => {
        this.rerolling.set(false);
        this.rerollError.set(err?.error?.detail ?? 'Erreur lors du reroll.');
      },
    });
  }

  closeRerollResult(): void {
    const result = this.rerollResult();
    this.rerollResult.set(null);
    if (result) {
      this.selectedCard.set(result);
    }
  }


  // ========================================
  //          MERGE CALLBACK
  // ========================================

  onMerged(): void {
    this.loadOwnedCards();
  }


  // ========================================
  //         DEACTIVATE (CREATIONS)
  // ========================================

  openDeactivateModal(card: Card): void {
    this.deactivateCardData.set(card);
    this.deactivateStep.set('info');
    this.deactivateError.set(null);
    this.showDeactivateModal.set(true);
  }

  closeDeactivateModal(): void {
    this.showDeactivateModal.set(false);
  }

  onDeactivateOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('deactivate-modal')) {
      this.closeDeactivateModal();
    }
  }

  confirmDeactivate(): void {
    const card = this.deactivateCardData();
    if (!card?.id || this.deactivating()) return;

    this.deactivating.set(true);
    this.deactivateError.set(null);

    this.meService.deactivateCard(card.id).subscribe({
      next: () => {
        this.deactivating.set(false);
        this.showDeactivateModal.set(false);
        this.createdCards.update(cards =>
          cards.map(c => c.id === card.id ? { ...c, active: false } : c)
        );
      },
      error: (err) => {
        this.deactivating.set(false);
        this.deactivateError.set(err?.error?.detail ?? 'Erreur lors de la désactivation.');
      },
    });
  }


  // ========================================
  //              HELPERS
  // ========================================

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  getTargetName(card: Card): string | null {
    if (!card.targetUserId) return null;
    const user = this.users().find(u => u.id === card.targetUserId);
    return user?.username ?? null;
  }
}
