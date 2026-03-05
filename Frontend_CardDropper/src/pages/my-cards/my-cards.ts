import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe, UpperCasePipe } from '@angular/common';
import { MeService } from '../../app/services/me.service';
import { CardService } from '../../app/services/card.service';
import { UserService } from '../../app/services/user.service';
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
  private readonly userService = inject(UserService);

  // ── Tabs ──
  protected readonly activeTab = signal<'cards' | 'merge' | 'creations' | 'targets'>('cards');

  // ── Data ──
  protected readonly ownedCards = signal<Card[]>([]);
  protected readonly createdCards = signal<Card[]>([]);
  protected readonly targetingCards = signal<Card[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(false);

  // ── Carousel ──
  protected readonly activeIndex = signal(0);

  protected readonly activeCard = computed(() => {
    const cards = this.ownedCards();
    if (cards.length === 0) return null;
    return cards[this.activeIndex()];
  });

  protected readonly canPrev = computed(() => this.activeIndex() > 0);
  protected readonly canNext = computed(() => this.activeIndex() < this.ownedCards().length - 1);

  // ── Use card modal ──
  protected readonly showUseModal = signal(false);
  protected readonly selectedTarget = signal<User | null>(null);
  protected readonly useStep = signal<'choose' | 'confirm'>('choose');
  protected readonly userSearch = signal('');

  protected readonly filteredUsers = computed(() => {
    const search = this.userSearch().toLowerCase().trim();
    const all = this.users();
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
    this.userService.getAll().subscribe({
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

  confirmUse(): void {
    const card = this.activeCard();
    const target = this.selectedTarget();
    if (!card || !target) return;

    console.log(`[USE CARD] Card "${card.name}" (id=${card.id}) used against "${target.username}" (id=${target.id})`);

    this.showUseModal.set(false);
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
