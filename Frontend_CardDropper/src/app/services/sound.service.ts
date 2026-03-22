import { Injectable, signal } from '@angular/core';
import { Rarity } from '../models';

const THEMES = ['Busi', 'Stofonde'];

const SOUND_FILES: Record<Rarity, string> = {
  [Rarity.COMMON]:    'Commune.mp3',
  [Rarity.RARE]:      'Rare.mp3',
  [Rarity.EPIC]:      'Epic.mp3',
  [Rarity.LEGENDARY]: 'Legendaire.mp3',
};

const STORAGE_KEY = 'cd_sound_muted';

@Injectable({ providedIn: 'root' })
export class SoundService {

  readonly muted = signal<boolean>(localStorage.getItem(STORAGE_KEY) === 'true');

  private activeTheme: string | null = null;

  pickTheme(): void {
    this.activeTheme = THEMES[Math.floor(Math.random() * THEMES.length)];
  }

  clearTheme(): void {
    this.activeTheme = null;
  }

  toggleMute(): void {
    const next = !this.muted();
    this.muted.set(next);
    localStorage.setItem(STORAGE_KEY, String(next));
  }

  play(rarity: Rarity): void {
    if (this.muted()) return;
    const theme = this.activeTheme ?? THEMES[0];
    const src = `assets/sounds/${theme}/${SOUND_FILES[rarity]}`;
    const audio = new Audio(src);
    audio.volume = 0.6;
    audio.play().catch(() => { /* autoplay policy — silently ignore */ });
  }
}
