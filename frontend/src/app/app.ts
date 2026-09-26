import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CurrentUserService } from './auth/current-user.service';
import { UserSwitcher } from './auth/user-switcher/user-switcher';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserSwitcher],
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly currentUser = inject(CurrentUserService);

  constructor() {
    this.currentUser.init();
  }
}
