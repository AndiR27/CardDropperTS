import { CanDeactivateFn } from '@angular/router';
import { TradeCardPage } from './trade-card';

export const canLeaveTrade: CanDeactivateFn<TradeCardPage> = (component) => {
  if (component.hasActiveSession()) {
    const leave = confirm('Vous êtes en plein échange, êtes-vous sûr de vouloir quitter ?');
    if (leave) {
      component.cancelTrade();
    }
    return leave;
  }
  return true;
};
