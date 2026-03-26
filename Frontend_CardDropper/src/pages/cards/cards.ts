import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../app/core/auth/auth.service';
import { CardService } from '../../app/services/card.service';
import { UserService } from '../../app/services/user.service';
import { MeService } from '../../app/services/me.service';
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
  private readonly meService = inject(MeService);

  // ── Data ──
  private readonly allCards = signal<Card[]>([]);
  protected readonly loading = signal(false);
  protected readonly users = signal<User[]>([]);
  private readonly userMap = signal<Map<number, string>>(new Map());

  // ── Pagination ──
  protected readonly currentPage = signal(0);
  protected readonly pageSize = 20;

  // ── Filters ──
  protected readonly searchName = signal('');
  protected readonly filterRarity = signal<Rarity | ''>('');
  protected readonly selectedTargets = signal<string[]>([]);
  protected readonly targetInput = signal('');
  protected readonly showTargetSuggestions = signal(false);
  protected readonly filterUnique = signal(false);
  protected readonly hideTargeted = signal(false);
  protected readonly showInactive = signal(false);
  protected readonly sortBy = signal<'' | 'name-asc' | 'name-desc' | 'rarity-asc' | 'rarity-desc'>('');
  protected readonly showNewest = signal(sessionStorage.getItem('cd-show-newest') === 'true');

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

  // ── Filtered cards (all pages) ──
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

    // Target filter (by selected usernames → userIds)
    const targets = this.selectedTargets();
    if (targets.length > 0) {
      const targetIds = new Set(
        targets.map(name => this.users().find(u => u.username === name)?.id).filter(id => id != null)
      );
      if (targetIds.size > 0) {
        cards = cards.filter(c => c.targetUserId !== null && targetIds.has(c.targetUserId));
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

    // Hide inactive cards unless explicitly shown
    if (!this.showInactive()) {
      cards = cards.filter(c => c.active);
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

    // Newest first (by id descending)
    if (this.showNewest()) {
      cards = [...cards].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
    }

    return cards;
  });

  // ── Client-side pagination ──
  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredCards().length / this.pageSize))
  );
  protected readonly totalElements = computed(() => this.filteredCards().length);

  // Clamp current page when filters reduce total pages
  protected readonly safePage = computed(() =>
    Math.min(this.currentPage(), this.totalPages() - 1)
  );

  protected readonly pagedCards = computed(() => {
    const page = this.safePage();
    const start = page * this.pageSize;
    return this.filteredCards().slice(start, start + this.pageSize);
  });

  // ── Target suggestions (alphabetical, excludes already selected) ──
  protected readonly targetSuggestions = computed(() => {
    const input = this.targetInput().toLowerCase().trim();
    const selected = new Set(this.selectedTargets());
    return this.users()
      .map(u => u.username)
      .filter(name => !selected.has(name) && (!input || name.toLowerCase().includes(input)))
      .sort((a, b) => a.localeCompare(b));
  });

  // ── Card count ──
  protected readonly cardCount = computed(() => this.filteredCards().length);
  protected readonly totalCount = computed(() => this.allCards().length);

  // ── Active filters ──
  protected readonly activeFilterCount = computed(() => {
    let count = 0;
    if (this.filterRarity()) count++;
    if (this.filterUnique()) count++;
    if (this.hideTargeted()) count++;
    if (this.selectedTargets().length > 0) count++;
    if (this.searchName()) count++;
    if (this.showInactive()) count++;
    if (this.showNewest()) count++;
    return count;
  });

  protected readonly hasActiveFilters = computed(() => this.activeFilterCount() > 0);

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
    this.currentPage.set(0);
  }

  // ── Target multi-select ──
  onTargetFocus(): void {
    this.showTargetSuggestions.set(true);
  }

  onTargetBlur(): void {
    setTimeout(() => this.showTargetSuggestions.set(false), 300);
  }

  selectTarget(username: string): void {
    if (!this.selectedTargets().includes(username)) {
      this.selectedTargets.update(list => [...list, username]);
    }
    this.targetInput.set('');
    this.showTargetSuggestions.set(false);
    this.currentPage.set(0);
  }

  removeTarget(username: string): void {
    this.selectedTargets.update(list => list.filter(n => n !== username));
    this.currentPage.set(0);
  }

  onTargetInputChange(value: string): void {
    this.targetInput.set(value);
    this.showTargetSuggestions.set(true);
  }

  clearAllTargets(): void {
    this.targetInput.set('');
    this.selectedTargets.set([]);
    this.currentPage.set(0);
  }

  // ── Newest toggle (session-persistent) ──
  toggleNewest(): void {
    const next = !this.showNewest();
    this.showNewest.set(next);
    sessionStorage.setItem('cd-show-newest', String(next));
    this.currentPage.set(0);
  }

  // ── Reset filters ──
  resetFilters(): void {
    this.searchName.set('');
    this.filterRarity.set('');
    this.filterUnique.set(false);
    this.hideTargeted.set(false);
    this.showInactive.set(false);
    this.selectedTargets.set([]);
    this.targetInput.set('');
    this.sortBy.set('');
    this.showNewest.set(false);
    sessionStorage.removeItem('cd-show-newest');
    this.currentPage.set(0);
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

  // ── Pagination navigation ──
  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.currentPage.set(page);
  }

  nextPage(): void {
    this.goToPage(this.currentPage() + 1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  prevPage(): void { this.goToPage(this.currentPage() - 1); }

  // ── Data loading ──
  private loadCards(): void {
    this.loading.set(true);
    this.cardService.getAll().subscribe({
      next: (cards) => {
        this.allCards.set(cards);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private loadUsers(): void {
    this.meService.getAllUsers().subscribe({
      next: (data) => {
        this.users.set(data);
        const map = new Map<number, string>();
        data.forEach(u => { if (u.id !== null) map.set(u.id, u.username); });
        this.userMap.set(map);
      },
    });
  }
}
