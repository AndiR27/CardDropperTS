import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MeService } from '../../../app/services/me.service';
import { AuthService } from '../../../app/core/auth/auth.service';
import { Card, Rarity } from '../../../app/models';

@Component({
  selector: 'app-add-card',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './add-card.html',
  styleUrl: './add-card.scss',
})
export class AddCard {

  back = output<void>();
  saved = output<string>();

  private readonly meService = inject(MeService);
  private readonly auth = inject(AuthService);

  // ── State ──
  imageFile = signal<File | null>(null);
  imagePreviewUrl = signal<string | null>(null);
  submitting = signal(false);
  dragOver = signal(false);

  // ── Form fields ──
  name = signal('');
  rarity = signal<Rarity>(Rarity.COMMON);
  uniqueCard = signal(false);
  description = signal('');
  targeting = signal(false);

  readonly rarities = Object.values(Rarity);

  // ── Image handling ──
  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.setImage(file);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files[0];
    if (file?.type.startsWith('image/')) this.setImage(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  private setImage(file: File): void {
    this.imageFile.set(file);
    const reader = new FileReader();
    reader.onload = () => this.imagePreviewUrl.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  removeImage(): void {
    this.imageFile.set(null);
    this.imagePreviewUrl.set(null);
  }

  // ── Save ──
  onSave(): void {
    const file = this.imageFile();
    if (!file || !this.name()) return;

    this.submitting.set(true);

    const card = {
      name: this.name(),
      description: this.description() || null,
      rarity: this.rarity(),
      dropRate: 0,
      uniqueCard: this.uniqueCard(),
      targetUserId: this.targeting() ? this.auth.userId : null,
      imageUrl: null,
    } as Card;

    this.meService.createCardWithImage(card, file).subscribe({
      next: () => {
        this.submitting.set(false);
        this.saved.emit(card.name);
      },
      error: (err) => {
        console.error('Failed to create card:', err);
        this.submitting.set(false);
      },
    });
  }

  onBack(): void {
    this.back.emit();
  }
}
