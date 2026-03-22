import { Rarity } from './rarity.enum';

export interface Card {
  id: number | null;
  name: string;
  imageUrl: string | null;
  rarity: Rarity;
  description: string | null;
  dropRate: number;
  uniqueCard: boolean;
  active: boolean;
  createdById: number | null;
  targetUserId: number | null;
}
