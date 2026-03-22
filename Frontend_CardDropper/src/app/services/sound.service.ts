import { Injectable, signal } from '@angular/core';
import { Rarity } from '../models';

const SOUND_MAP: Record<Rarity, string> = {
  [Rarity.COMMON]:    'assets/sounds/Commune.mp3',
  [Rarity.RARE]:      'assets/sounds/Rare.mp3',
  [Rarity.EPIC]:      'assets/sounds/Epic.mp3',
  [Rarity.LEGENDARY]: 'assets/sounds/Legendaire.mp3',
};

const STORAGE_KEY = 'cd_sound_muted';

@Injectable({ providedIn: 'root' })
export class SoundService {

  readonly muted = signal<boolean>(localStorage.getItem(STORAGE_KEY) === 'true');

  toggleMute(): void {
    const next = !this.muted();
    this.muted.set(next);
    localStorage.setItem(STORAGE_KEY, String(next));
  }

  play(rarity: Rarity): void {
    if (this.muted()) return;
    const src = SOUND_MAP[rarity];
    if (!src) return;
    const audio = new Audio(src);
    audio.volume = 0.6;
    audio.play().catch(() => { /* autoplay policy — silently ignore */ });
  }
}
