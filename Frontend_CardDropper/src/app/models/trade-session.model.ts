import type { Card } from './card.model';

export type TradeSessionStatus = 'PENDING' | 'ACTIVE' | 'LOCKED' | 'COMPLETED' | 'CANCELLED';

export interface TradeSession {
  id: string;
  status: TradeSessionStatus;
  initiatorUsername: string;
  receiverUsername: string;
  initiatorCard: Card | null;
  receiverCard: Card | null;
  initiatorLocked: boolean;
  receiverLocked: boolean;
  createdAt: string;
}

export interface OnlineUser {
  userId: number;
  username: string;
}
