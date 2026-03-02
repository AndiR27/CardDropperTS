import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '',        loadComponent: () => import('../pages/home/home').then(m => m.HomePage) },
  { path: 'cards',   loadComponent: () => import('../pages/cards/cards').then(m => m.CardsPage) },
  { path: 'my-cards', loadComponent: () => import('../pages/my-cards/my-cards').then(m => m.MyCardsPage), canActivate: [authGuard] },
  { path: 'create',  loadComponent: () => import('../pages/create/create-card').then(m => m.CreateCardPage), canActivate: [authGuard] },
  { path: 'packs',   loadComponent: () => import('../pages/packs/packs').then(m => m.PacksPage), canActivate: [authGuard] },
  { path: 'test',    loadComponent: () => import('../pages/test/test').then(m => m.TestPage) },
];
