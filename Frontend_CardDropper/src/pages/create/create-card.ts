import { Component, computed, HostListener, inject, signal, viewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CardPreview, CardType, CardClass } from './card-preview/card-preview';
import { AddCard } from './add-card/add-card';
import { AuthService } from '../../app/core/auth/auth.service';
import { CardService } from '../../app/services/card.service';
import { Card, Rarity } from '../../app/models';

@Component({
  selector: 'app-create-card',
  standalone: true,
  imports: [FormsModule, CardPreview, AddCard],
  templateUrl: './create-card.html',
  styleUrl: './create-card.scss',
})
export class CreateCardPage {

  private readonly auth = inject(AuthService);
  private readonly cardService = inject(CardService);

  // ── Mode ──
  mode = signal<'choose' | 'add' | 'build'>('choose');

  // ── Unsaved work detection ──
  hasUnsavedWork = computed(() => this.mode() !== 'choose');

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.hasUnsavedWork()) {
      event.preventDefault();
    }
  }

  // ── Flow state ──
  step = signal<'edit' | 'confirm'>('edit');
  creating = signal(false);
  submitting = signal(false);
  renderedImageBlob = signal<Blob | null>(null);
  renderedImageUrl = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // ── Form state (signals for live preview) ──
  cardType     = signal<CardType>('spell');
  cardClass    = signal<CardClass>('neutral');
  name         = signal('');
  description  = signal('');
  rarity       = signal<Rarity>(Rarity.COMMON);
  cardImageUrl = signal<string | null>(null);
  targeting    = signal(false);

  // ── Raw uploaded file (kept separate from preview data URL) ──
  private uploadedFile: File | null = null;

  // ── Computed target name (the creator targets themselves) ──
  targetName = computed(() =>
    this.targeting() ? (this.auth.username ?? '') : ''
  );

  // ── Card preview ref ──
  cardPreview = viewChild<CardPreview>(CardPreview);

  // ── Option lists ──
  readonly cardTypes: CardType[] = ['spell', 'minion', 'weapon'];

  readonly cardClasses: CardClass[] = [
    'neutral', 'druid', 'hunter', 'mage', 'paladin',
    'priest', 'rogue', 'shaman', 'warlock', 'warrior',
    'deathknight', 'demonhunter',
  ];

  readonly rarities = Object.values(Rarity);

  // ── Image upload ──
  private fileInput = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    this.uploadedFile = file;
    const reader = new FileReader();
    reader.onload = () => this.cardImageUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeImage(): void {
    this.cardImageUrl.set(null);
    this.uploadedFile = null;
    const input = this.fileInput();
    if (input) {
      input.nativeElement.value = '';
    }
  }

  // ── Réinitialise le formulaire après création réussie ──
  private resetForm(): void {
    const prevUrl = this.renderedImageUrl();
    if (prevUrl) URL.revokeObjectURL(prevUrl);

    this.mode.set('choose');
    this.step.set('edit');
    this.name.set('');
    this.description.set('');
    this.rarity.set(Rarity.COMMON);
    this.cardType.set('spell');
    this.cardClass.set('neutral');
    this.cardImageUrl.set(null);
    this.targeting.set(false);
    this.uploadedFile = null;
    this.renderedImageBlob.set(null);
    this.renderedImageUrl.set(null);
  }

  // ── Create flow ──
  async onCreateClick(): Promise<void> {
    const preview = this.cardPreview();
    if (!preview) return;

    this.creating.set(true);
    try {
      const blob = await preview.captureImage();
      this.renderedImageBlob.set(blob);

      // Revoke previous URL if any
      const prevUrl = this.renderedImageUrl();
      if (prevUrl) URL.revokeObjectURL(prevUrl);

      this.renderedImageUrl.set(URL.createObjectURL(blob));
      this.step.set('confirm');
    } catch (err) {
      console.error('Failed to capture card image:', err);
    } finally {
      this.creating.set(false);
    }
  }

  onBack(): void {
    this.step.set('edit');
  }

  onConfirm(): void {
    const blob = this.renderedImageBlob();
    if (!blob) return;

    this.submitting.set(true);

    const card: Partial<Card> = {
      name: this.name(),
      description: this.description() || null,
      rarity: this.rarity(),
      dropRate: 0,
      uniqueCard: false,
      imageUrl: null,
    };

    const filename = `${this.name() || 'card'}.png`;

    this.cardService.createWithImage(card, blob, filename).subscribe({
      next: () => {
        this.submitting.set(false);
        this.resetForm();
        this.successMessage.set(`La carte "${card.name}" a été forgée avec succès !`);
      },
      error: (err) => {
        console.error('Failed to create card:', err);
        this.submitting.set(false);
      },
    });
  }
}
