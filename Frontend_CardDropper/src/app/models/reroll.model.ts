import type { Card } from './card.model';
import type { Rarity } from './rarity.enum';

export interface RerollResponse {
  sacrificedCard: Card;
  receivedCard: Card;
}

export interface RerollCooldown {
  rarity: Rarity;
  lastRerollAt: string;
  availableAt: string;
}
