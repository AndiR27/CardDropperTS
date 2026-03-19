import { Card } from './card.model';

export interface User {
  id: number | null;
  username: string;
  email: string;
  passwordHash: string | null;
  admin: boolean;
  cardsOwned: Card[];
  cardsCreated: Card[];
  cardsTargeting: Card[];
}
