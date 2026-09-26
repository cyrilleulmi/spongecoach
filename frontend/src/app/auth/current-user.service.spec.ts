import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrentUserService, USER_COOKIE } from './current-user.service';
import { AppUser, CurrentUser } from './user.model';

const ADMIN: AppUser = { id: 'u-admin', name: 'Admin', role: 'SYS_ADMIN', playerId: null };
const COACH: AppUser = { id: 'u-cyrille', name: 'Cyrille', role: 'COACH', playerId: null };
const CARMELA: AppUser = { id: 'u-carmela', name: 'Carmela', role: 'PLAYER', playerId: 'p-carmela' };

function me(user: AppUser, lineIds: string[] = []): CurrentUser {
  return { ...user, teamId: user.role === 'SYS_ADMIN' ? null : 'team', lineIds };
}

function clearCookie(): void {
  document.cookie = `${USER_COOKIE}=; path=/; max-age=0`;
}

describe('CurrentUserService', () => {
  let service: CurrentUserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    clearCookie();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CurrentUserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    clearCookie();
  });

  // spec: ui.default-user
  it('picks the SysAdmin on a first visit and remembers the choice in the cookie', () => {
    service.init();
    httpMock.expectOne('/api/users').flush([CARMELA, COACH, ADMIN]);
    httpMock.expectOne('/api/me').flush(me(ADMIN));

    expect(service.me()?.id).toBe(ADMIN.id);
    expect(document.cookie).toContain(`${USER_COOKIE}=${ADMIN.id}`);
  });

  // spec: ui.default-user
  it('keeps the User already chosen', () => {
    document.cookie = `${USER_COOKIE}=${CARMELA.id}; path=/`;
    service.init();
    httpMock.expectOne('/api/users').flush([ADMIN, CARMELA]);
    httpMock.expectOne('/api/me').flush(me(CARMELA));

    expect(service.me()?.id).toBe(CARMELA.id);
  });

  it('lets a coach edit everything', () => {
    service.me.set(me(COACH));
    expect(service.isCoach()).toBe(true);
    expect(service.canEditLine('any-line')).toBe(true);
    expect(service.canEditPlayer('any-player')).toBe(true);
  });

  it('lets a Player edit only their own Lines and themselves', () => {
    service.me.set(me(CARMELA, ['line-kiwi']));
    expect(service.isCoach()).toBe(false);
    expect(service.canEditLine('line-kiwi')).toBe(true);
    expect(service.canEditLine('line-baeri')).toBe(false);
    expect(service.canEditPlayer('p-carmela')).toBe(true);
    expect(service.canEditPlayer('p-rahel')).toBe(false);
  });
});
