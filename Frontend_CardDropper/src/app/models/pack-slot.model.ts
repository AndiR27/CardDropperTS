import { Rarity } from './rarity.enum';

export interface PackSlot {
  id: number | null;
  fixedRarity: Rarity | null;
  rarityWeights: Partial<Record<Rarity, number>> | null;
}
