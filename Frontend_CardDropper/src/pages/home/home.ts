import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../app/core/auth/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomePage {
  protected readonly auth = inject(AuthService);
  heroLoaded = false;

  login(): void {
    this.auth.login();
  }
}
