import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth/auth.guard';
import { canLeaveCreate } from '../pages/create/create-card.guard';
import { canLeaveTrade } from '../pages/trade/trade-card/trade-card.guard';

export const routes: Routes = [
  { path: '',        loadComponent: () => import('../pages/home/home').then(m => m.HomePage) },
  { path: 'cards',   loadComponent: () => import('../pages/cards/cards').then(m => m.CardsPage) },
  { path: 'my-cards', loadComponent: () => import('../pages/my-cards/my-cards').then(m => m.MyCardsPage), canActivate: [authGuard] },
  { path: 'create',  loadComponent: () => import('../pages/create/create-card').then(m => m.CreateCardPage), canActivate: [authGuard], canDeactivate: [canLeaveCreate] },
  { path: 'packs',   loadComponent: () => import('../pages/packs/packs').then(m => m.PacksPage), canActivate: [authGuard] },
  { path: 'trade',   loadComponent: () => import('../pages/trade/trade').then(m => m.TradePage), canActivate: [authGuard] },
  { path: 'trade/session/:id', loadComponent: () => import('../pages/trade/trade-card/trade-card').then(m => m.TradeCardPage), canActivate: [authGuard], canDeactivate: [canLeaveTrade] },
  { path: 'admin',   loadComponent: () => import('../pages/admin/admin-view').then(m => m.AdminViewPage), canActivate: [adminGuard] },
  { path: 'test',    loadComponent: () => import('../pages/test/test').then(m => m.TestPage) },
];
