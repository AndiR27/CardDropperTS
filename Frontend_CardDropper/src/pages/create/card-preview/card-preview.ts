import { Component, computed, ElementRef, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ArcText } from './arc-text';
import { Rarity } from '../../../app/models';
import html2canvas from 'html2canvas-pro';

export type CardType = 'spell' | 'minion' | 'weapon';

export type CardClass =
  | 'neutral' | 'druid' | 'hunter' | 'mage' | 'paladin'
  | 'priest' | 'rogue' | 'shaman' | 'warlock' | 'warrior'
  | 'deathknight' | 'demonhunter';

@Component({
  selector: 'app-card-preview',
  standalone: true,
  imports: [FormsModule, ArcText],
  templateUrl: './card-preview.html',
  styleUrl: './card-preview.scss',
})
export class CardPreview {

  // ── Inputs ──
  cardType     = input<CardType>('spell');
  cardClass    = input<CardClass>('neutral');
  name         = input<string>('');
  description  = input<string>('');
  rarity       = input<Rarity>(Rarity.COMMON);
  cardImageUrl = input<string | null>(null);
  targetName   = input<string>('');
  locked       = input<boolean>(false);

  // ── Element ref for capture ──
  cardEl = viewChild<ElementRef<HTMLDivElement>>('cardEl');

  // ── Image zoom / pan state ──
  zoom   = signal(1);
  panX   = signal(0);
  panY   = signal(0);
  private dragging = false;
  private lastX = 0;
  private lastY = 0;

  // ── Computed helpers ──
  isSpell  = computed(() => this.cardType() === 'spell');
  isMinion = computed(() => this.cardType() === 'minion');
  isWeapon = computed(() => this.cardType() === 'weapon');

  isLegendary = computed(() => this.rarity() === Rarity.LEGENDARY);

  artTransform = computed(() =>
    `translate(${this.panX()}px, ${this.panY()}px) scale(${this.zoom()})`
  );

  // ── Computed asset paths ──

  baseFrameUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/${this.cardClass()}-${this.cardType()}.png`
  );

  nameBarUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/name-${this.cardType()}.png`
  );

  rarityMaskUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/rarity-mask-${this.cardType()}.png`
  );

  rarityCrystalUrl = computed(() =>
    `assets/cardsCreation/rarity/${this.rarity().toLowerCase()}-crystal.png`
  );

  dragonUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/dragon-${this.cardType()}.png`
  );

  descAreaUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/description-area-${this.cardType()}.png`
  );

  // All types now have target-{type}.png
  targetBannerUrl = computed(() =>
    `assets/cardsCreation/${this.cardType()}/target-${this.cardType()}.png`
  );

  // ── Dynamic name font size (shrinks gradually after 15 chars, min 14) ──
  nameFontSize = computed(() => {
    const len = this.name().length;
    if (len <= 15) return 30;
    return Math.max(14, 30 - (len - 15) * 0.7);
  });

  // ── Dynamic description font size (shrinks after 20 chars, min 7) ──
  descFontSize = computed(() => {
    const len = this.description().length;
    if (len <= 20) return 16;
    return Math.max(7, 16 - (len - 20) * 0.15);
  });

  // ── Font embedding cache (fetched once, reused across captures) ──
  private fontStyleCache: string | null = null;

  private async buildFontStyles(): Promise<string> {
    if (this.fontStyleCache) return this.fontStyleCache;

    const fonts = [
      { family: 'BlizzardGlobal', url: 'assets/fonts/BlizzardGlobal.ttf', format: 'truetype', weight: 'normal' },
      { family: 'GBJenLei', url: 'assets/fonts/GBJenLei/GBJenLei-Medium.woff2', format: 'woff2', weight: '500' },
    ];

    const rules: string[] = [];
    for (const f of fonts) {
      const resp = await fetch(f.url);
      const buf = await resp.arrayBuffer();
      const bytes = new Uint8Array(buf);
      let binary = '';
      for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
      }
      const base64 = btoa(binary);
      rules.push(`@font-face { font-family: '${f.family}'; src: url('data:font/${f.format};base64,${base64}') format('${f.format}'); font-weight: ${f.weight}; font-style: normal; }`);
    }

    this.fontStyleCache = rules.join('\n');
    return this.fontStyleCache;
  }

  // ── Capture card as image ──
  async captureImage(): Promise<Blob> {
    const el = this.cardEl();
    if (!el) throw new Error('Card element not available');

    await document.fonts.ready;
    const fontStyles = await this.buildFontStyles();

    const canvas = await html2canvas(el.nativeElement, {
      useCORS: true,
      scale: 3,
      backgroundColor: null,
      onclone: async (clonedDoc: Document) => {
        const style = clonedDoc.createElement('style');
        style.textContent = fontStyles;
        clonedDoc.head.appendChild(style);
        await clonedDoc.fonts.ready;
      },
    });

    return new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        blob => blob ? resolve(blob) : reject(new Error('Canvas toBlob failed')),
        'image/png'
      );
    });
  }

  // ── Zoom ──
  onWheel(event: WheelEvent): void {
    if (this.locked()) return;
    event.preventDefault();
    const delta = event.deltaY > 0 ? -0.05 : 0.05;
    this.zoom.set(Math.max(0.5, Math.min(3, this.zoom() + delta)));
  }

  setZoom(value: number): void {
    if (this.locked()) return;
    this.zoom.set(Math.max(0.5, Math.min(3, value)));
  }

  // ── Pan (drag) ──
  onMouseDown(event: MouseEvent): void {
    if (this.locked()) return;
    this.dragging = true;
    this.lastX = event.clientX;
    this.lastY = event.clientY;
    event.preventDefault();
  }

  onMouseMove(event: MouseEvent): void {
    if (!this.dragging) return;
    this.panX.update(v => v + (event.clientX - this.lastX));
    this.panY.update(v => v + (event.clientY - this.lastY));
    this.lastX = event.clientX;
    this.lastY = event.clientY;
  }

  onMouseUp(): void {
    this.dragging = false;
  }

  resetPosition(): void {
    this.zoom.set(1);
    this.panX.set(0);
    this.panY.set(0);
  }
}
