import { Card } from './card.model';

export interface User {
  id: number | null;
  username: string;
  email: string;
  passwordHash: string | null;
  cardsOwned: Card[];
  cardsCreated: Card[];
  cardsTargeting: Card[];
}
