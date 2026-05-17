import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { signal } from '@angular/core';
import { UserSwitcherComponent } from './user-switcher.component';
import { AuthService, MockUser } from './auth.service';

const makeUser = (email: string, role?: string): MockUser =>
  ({ personId: '1', email, clubMemberships: [] }) as unknown as MockUser;

describe('UserSwitcherComponent', () => {
  let fixture: ComponentFixture<UserSwitcherComponent>;
  let availableUsersSignal: ReturnType<typeof signal<MockUser[]>>;
  let currentUserSignal: ReturnType<typeof signal<MockUser | null>>;

  beforeEach(async () => {
    availableUsersSignal = signal<MockUser[]>([
      makeUser('admin@test.ch'),
      makeUser('user@test.ch'),
    ]);
    currentUserSignal = signal<MockUser | null>(null);

    await TestBed.configureTestingModule({
      imports: [UserSwitcherComponent],
      providers: [{
        provide: AuthService,
        useValue: {
          currentUser: currentUserSignal,
          availableUsers: availableUsersSignal,
          loadMockUsers: () => {},
          selectUser: (u: MockUser) => currentUserSignal.set(u),
          isAdmin: signal(false),
        },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(UserSwitcherComponent);
    fixture.detectChanges();
  });

  it('renders dropdown items with email text when role field is absent', () => {
    // Use triggerEventHandler to avoid @HostListener('document:click') closing the dropdown
    fixture.debugElement.query(By.css('.user-btn')).triggerEventHandler('click', null);

    // Before fix: user.role.toLowerCase() throws TypeError and items don't render
    expect(() => fixture.detectChanges()).not.toThrow();

    const emailSpans: NodeListOf<HTMLElement> =
      fixture.nativeElement.querySelectorAll('.item-email');
    expect(emailSpans.length).toBe(2);
    expect(emailSpans[0].textContent?.trim()).toBe('admin@test.ch');
    expect(emailSpans[1].textContent?.trim()).toBe('user@test.ch');
  });
});
