import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import {
  faDungeon,
  faLayerGroup,
  faClone,
  faHammer,
  faGift,
  faRightLeft,
  faShieldHalved,
} from '@fortawesome/free-solid-svg-icons';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, FaIconComponent],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  protected readonly auth = inject(AuthService);
  protected readonly mobileMenuOpen = signal(false);

  readonly icons = {
    taverne:     faDungeon,
    collection:  faLayerGroup,
    mesCartes:   faClone,
    forge:       faHammer,
    packs:       faGift,
    echange:     faRightLeft,
    admin:       faShieldHalved,
  };

  toggleMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  closeMenu(): void {
    this.mobileMenuOpen.set(false);
  }
}
