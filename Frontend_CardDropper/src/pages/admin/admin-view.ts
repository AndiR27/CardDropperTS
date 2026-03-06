import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../app/services/admin.service';
import { CardService } from '../../app/services/card.service';
import { Card, PackSlot, PackTemplate, User, Rarity } from '../../app/models';
import { PackTemplateSlot } from '../../app/models/pack-template-slot.model';

@Component({
  selector: 'app-admin-view',
  standalone: true,
  imports: [FormsModule, LowerCasePipe],
  templateUrl: './admin-view.html',
  styleUrl: './admin-view.scss',
})
export class AdminViewPage implements OnInit {

  private readonly admin = inject(AdminService);
  private readonly cardService = inject(CardService);

  // ── Tabs ──
  protected readonly activeTab = signal<'packs' | 'users' | 'cards' | 'others'>('packs');

  // ── Data ──
  protected readonly packSlots = signal<PackSlot[]>([]);
  protected readonly packTemplates = signal<PackTemplate[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly cards = signal<Card[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);

  // ── New Pack Slot form ──
  newSlotName = '';
  newSlotFixedRarity: Rarity | null = null;
  newSlotWeights: Record<string, number> = this.defaultWeights();
  readonly rarities = Object.values(Rarity);

  // ── New Pack Template form ──
  newTemplateName = '';
  templateSlotRows: { slotId: number | null; count: number }[] = [];

  // ── Expanded template ──
  protected readonly expandedTemplateId = signal<number | null>(null);

  // ── Template total cards ──
  protected templateTotalCards = computed(() => {
    return (tpl: PackTemplate) => tpl.slots.reduce((sum, s) => sum + s.count, 0);
  });

  ngOnInit(): void {
    this.loadAll();
  }

  // ====== Loaders ======

  loadAll(): void {
    this.loading.set(true);
    this.error.set(null);

    this.admin.getPackSlots().subscribe({
      next: slots => this.packSlots.set(slots),
      error: err => this.handleError('Pack Slots', err),
    });

    this.admin.getPackTemplates().subscribe({
      next: templates => this.packTemplates.set(templates),
      error: err => this.handleError('Pack Templates', err),
    });

    this.admin.getUsers().subscribe({
      next: users => this.users.set(users),
      error: err => this.handleError('Users', err),
    });

    this.cardService.getAll().subscribe({
      next: cards => {
        this.cards.set(cards);
        this.loading.set(false);
      },
      error: err => {
        this.handleError('Cards', err);
        this.loading.set(false);
      },
    });
  }

  // ====== Pack Slot actions ======

  createSlot(): void {
    if (!this.newSlotName.trim()) return;

    const slot: PackSlot = {
      id: null,
      name: this.newSlotName.trim(),
      fixedRarity: this.newSlotFixedRarity,
      rarityWeights: this.newSlotFixedRarity ? null : { ...this.newSlotWeights },
    };

    this.admin.createPackSlot(slot).subscribe({
      next: () => {
        this.newSlotName = '';
        this.newSlotFixedRarity = null;
        this.newSlotWeights = this.defaultWeights();
        this.admin.getPackSlots().subscribe(s => this.packSlots.set(s));
      },
      error: err => this.handleError('Create Slot', err),
    });
  }

  // ====== Pack Template actions ======

  addTemplateSlotRow(): void {
    this.templateSlotRows.push({ slotId: null, count: 1 });
  }

  removeTemplateSlotRow(index: number): void {
    this.templateSlotRows.splice(index, 1);
  }

  createTemplate(): void {
    if (!this.newTemplateName.trim()) return;

    const slots: PackTemplateSlot[] = this.templateSlotRows
      .filter(r => r.slotId !== null)
      .map(r => ({
        id: null,
        slotId: r.slotId!,
        slotName: '',
        count: r.count,
      }));

    const template: PackTemplate = {
      id: null,
      name: this.newTemplateName.trim(),
      slots,
    };

    this.admin.createPackTemplate(template).subscribe({
      next: () => {
        this.newTemplateName = '';
        this.templateSlotRows = [];
        this.admin.getPackTemplates().subscribe(t => this.packTemplates.set(t));
      },
      error: err => this.handleError('Create Template', err),
    });
  }

  deleteTemplate(id: number): void {
    this.admin.deletePackTemplate(id).subscribe({
      next: () => this.admin.getPackTemplates().subscribe(t => this.packTemplates.set(t)),
      error: err => this.handleError('Delete Template', err),
    });
  }

  toggleTemplate(id: number): void {
    this.expandedTemplateId.set(this.expandedTemplateId() === id ? null : id);
  }

  // ====== Card helpers ======

  getImageUrl(card: Card): string | null {
    return this.cardService.imageUrl(card);
  }

  cardCountByRarity(rarity: Rarity): number {
    return this.cards().filter(c => c.rarity === rarity).length;
  }

  // ====== Slot helpers ======

  slotWeightsSummary(slot: PackSlot): string {
    if (!slot.rarityWeights) return '';
    return Object.entries(slot.rarityWeights)
      .map(([r, w]) => `${r}: ${w}%`)
      .join(' · ');
  }

  private defaultWeights(): Record<string, number> {
    return { COMMON: 70, RARE: 25, EPIC: 4, LEGENDARY: 1 };
  }

  // ====== Helpers ======

  private handleError(context: string, err: unknown): void {
    const status = (err as { status?: number })?.status;
    if (status === 403) {
      this.error.set('Accès refusé — rôle ADMIN requis.');
    } else {
      this.error.set(`Erreur ${context}: ${status ?? 'inconnue'}`);
    }
  }
}
