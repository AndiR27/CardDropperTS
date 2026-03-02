import { Rarity } from './rarity.enum';

export interface Card {
  id: number | null;
  name: string;
  imageUrl: string | null;
  rarity: Rarity;
  description: string | null;
  dropRate: number;
  uniqueCard: boolean;
  userId: number | null;
  createdById: number | null;
  targetUserId: number | null;
}
