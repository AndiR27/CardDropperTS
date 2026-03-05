import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../app/core/auth/auth.service';
import { CardService } from '../../app/services/card.service';
import { UserService } from '../../app/services/user.service';
import { Card, Rarity } from '../../app/models';
import type { User } from '../../app/models';

@Component({
  selector: 'app-cards',
  standalone: true,
  imports: [LowerCasePipe, FormsModule],
  templateUrl: './cards.html',
  styleUrl: './cards.scss',
})
export class CardsPage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly cardService = inject(CardService);
  private readonly userService = inject(UserService);

  // ── Data ──
  private readonly allCards = signal<Card[]>([]);
  protected readonly loading = signal(false);
  protected readonly users = signal<User[]>([]);
  private readonly userMap = signal<Map<number, string>>(new Map());

  // ── Filters ──
  protected readonly searchName = signal('');
  protected readonly filterRarity = signal<Rarity | ''>('');
  protected readonly filterTarget = signal('');
  protected readonly targetInput = signal('');
  protected readonly showTargetSuggestions = signal(false);
  protected readonly filterUnique = signal(false);
  protected readonly hideTargeted = signal(false);
  protected readonly sortBy = signal<'' | 'name-asc' | 'name-desc' | 'rarity-asc' | 'rarity-desc'>('');

  // ── Zoom overlay ──
  protected readonly zoomedCard = signal<Card | null>(null);

  // ── Rarity options with images ──
  readonly rarityOptions: { value: Rarity | ''; label: string; image?: string }[] = [
    { value: '', label: 'Toutes' },
    { value: Rarity.COMMON, label: 'Commune', image: 'assets/cardsCreation/rarity/common-crystal.png' },
    { value: Rarity.RARE, label: 'Rare', image: 'assets/cardsCreation/rarity/rare-crystal.png' },
    { value: Rarity.EPIC, label: 'Épique', image: 'assets/cardsCreation/rarity/epic-crystal.png' },
    { value: Rarity.LEGENDARY, label: 'Légendaire', image: 'assets/cardsCreation/rarity/legendary-crystal.png' },
  ];

  // ── Filtered cards ──
  protected readonly filteredCards = computed(() => {
    let cards = this.allCards();

    // Non-logged users: hide targeted cards
    if (!this.auth.isAuthenticated) {
      cards = cards.filter(c => c.targetUserId === null);
    }

    // Name search
    const name = this.searchName().toLowerCase().trim();
    if (name) {
      cards = cards.filter(c => c.name.toLowerCase().includes(name));
    }

    // Rarity filter
    const rarity = this.filterRarity();
    if (rarity) {
      cards = cards.filter(c => c.rarity === rarity);
    }

    // Target filter (by username → userId)
    const target = this.filterTarget();
    if (target) {
      const targetUser = this.users().find(u => u.username === target);
      if (targetUser) {
        cards = cards.filter(c => c.targetUserId === targetUser.id);
      } else {
        cards = [];
      }
    }

    // Unique filter
    if (this.filterUnique()) {
      cards = cards.filter(c => c.uniqueCard);
    }

    // Hide targeted cards
    if (this.hideTargeted()) {
      cards = cards.filter(c => c.targetUserId === null);
    }

    // Sorting
    const sort = this.sortBy();
    if (sort) {
      const rarityOrder = { COMMON: 0, RARE: 1, EPIC: 2, LEGENDARY: 3 };
      cards = [...cards].sort((a, b) => {
        switch (sort) {
          case 'name-asc': return a.name.localeCompare(b.name);
          case 'name-desc': return b.name.localeCompare(a.name);
          case 'rarity-asc': return (rarityOrder[a.rarity] ?? 0) - (rarityOrder[b.rarity] ?? 0);
          case 'rarity-desc': return (rarityOrder[b.rarity] ?? 0) - (rarityOrder[a.rarity] ?? 0);
          default: return 0;
        }
      });
    }

    return cards;
  });

  // ── Target suggestions (autocomplete) ──
  protected readonly targetSuggestions = computed(() => {
    const input = this.targetInput().toLowerCase().trim();
    if (!input) return this.users().map(u => u.username).slice(0, 8);
    return this.users()
      .map(u => u.username)
      .filter(name => name.toLowerCase().includes(input))
      .slice(0, 8);
  });

  // ── Card count ──
  protected readonly cardCount = computed(() => this.filteredCards().length);
  protected readonly totalCount = computed(() => this.allCards().length);

  ngOnInit(): void {
    this.loadCards();
    if (this.auth.isAuthenticated) {
      this.loadUsers();
    }
  }

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  getCreatorName(card: Card): string {
    if (!card.createdById) return 'Inconnu';
    return this.userMap().get(card.createdById) ?? 'Inconnu';
  }

  getTargetName(card: Card): string | null {
    if (!card.targetUserId) return null;
    return this.userMap().get(card.targetUserId) ?? null;
  }

  // ── Rarity filter ──
  selectRarity(value: Rarity | ''): void {
    this.filterRarity.set(value);
  }

  // ── Target autocomplete ──
  onTargetFocus(): void {
    this.showTargetSuggestions.set(true);
  }

  onTargetBlur(): void {
    // Delay to allow click on suggestion
    setTimeout(() => this.showTargetSuggestions.set(false), 200);
  }

  selectTarget(username: string): void {
    this.targetInput.set(username);
    this.filterTarget.set(username);
    this.showTargetSuggestions.set(false);
  }

  onTargetInputChange(value: string): void {
    this.targetInput.set(value);
    if (!value) {
      this.filterTarget.set('');
    }
    this.showTargetSuggestions.set(true);
  }

  clearTarget(): void {
    this.targetInput.set('');
    this.filterTarget.set('');
  }

  // ── Zoom ──
  openZoom(card: Card): void {
    this.zoomedCard.set(card);
  }

  closeZoom(): void {
    this.zoomedCard.set(null);
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('zoom-overlay')) {
      this.closeZoom();
    }
  }

  // ── Data loading ──
  private loadCards(): void {
    this.loading.set(true);
    this.cardService.getAll().subscribe({
      next: (data) => { this.allCards.set(data); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  private loadUsers(): void {
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users.set(data);
        const map = new Map<number, string>();
        data.forEach(u => { if (u.id !== null) map.set(u.id, u.username); });
        this.userMap.set(map);
      },
    });
  }
}
