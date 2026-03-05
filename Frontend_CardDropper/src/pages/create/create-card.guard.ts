import { CanDeactivateFn } from '@angular/router';
import { CreateCardPage } from './create-card';

export const canLeaveCreate: CanDeactivateFn<CreateCardPage> = (component) => {
  if (component.hasUnsavedWork()) {
    return confirm('Êtes-vous sûr de vouloir quitter la page ? Les modifications non sauvegardées seront perdues.');
  }
  return true;
};
