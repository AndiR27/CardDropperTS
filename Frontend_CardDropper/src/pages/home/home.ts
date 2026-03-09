import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { AuthService } from '../../app/core/auth/auth.service';
import { LiveFeedService } from '../../app/services/live-feed.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, LowerCasePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomePage implements OnInit, OnDestroy {
  protected readonly auth = inject(AuthService);
  protected readonly liveFeed = inject(LiveFeedService);
  heroLoaded = false;

  ngOnInit(): void {
    if (this.auth.isAuthenticated) {
      this.liveFeed.connect();
    }
  }

  ngOnDestroy(): void {
    this.liveFeed.disconnect();
  }

  login(): void {
    this.auth.login();
  }

  formatEventDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();

    const isToday =
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate();

    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const time = `${hours}h${minutes}`;

    if (isToday) {
      return `Aujourd'hui, à ${time}`;
    }

    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}, à ${time}`;
  }
}
