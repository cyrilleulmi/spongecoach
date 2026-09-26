import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUserService } from '../current-user.service';
import { AppUser } from '../user.model';
import { UserSwitcher } from './user-switcher';

const USERS: AppUser[] = [
  { id: 'u-carmela', name: 'Carmela', role: 'PLAYER', playerId: 'p-carmela' },
  { id: 'u-samuel', name: 'Samuel', role: 'COACH', playerId: null },
  { id: 'u-anita', name: 'Anita', role: 'COACH', playerId: 'p-anita' },
  { id: 'u-admin', name: 'Admin', role: 'SYS_ADMIN', playerId: null },
];

describe('UserSwitcher', () => {
  // spec: ui.user-switcher
  it('lists every User grouped by Role, and switching reloads as the chosen User', async () => {
    await TestBed.configureTestingModule({
      imports: [UserSwitcher],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const currentUser = TestBed.inject(CurrentUserService);
    currentUser.users.set(USERS);
    currentUser.me.set({ ...USERS[3], teamId: null, lineIds: [] });
    const switchTo = jest.spyOn(currentUser, 'switchTo').mockImplementation(() => undefined);

    const fixture = TestBed.createComponent(UserSwitcher);
    fixture.detectChanges();

    const groups = Array.from(fixture.nativeElement.querySelectorAll('optgroup')) as HTMLOptGroupElement[];
    expect(groups.map((g) => g.label)).toEqual(['Admin', 'Trainer', 'Spieler']);
    expect(Array.from(groups[1].querySelectorAll('option')).map((o) => o.textContent?.trim())).toEqual([
      'Anita',
      'Samuel',
    ]);

    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect(select.value).toBe('u-admin');
    select.value = 'u-carmela';
    select.dispatchEvent(new Event('change'));

    expect(switchTo).toHaveBeenCalledWith('u-carmela');
  });
});
