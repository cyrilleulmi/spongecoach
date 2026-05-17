import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClubDetailComponent } from './club-detail.component';
import { AuthService, MockUser, ClubMembership } from '../../core/auth/auth.service';

const CLUB_ID = 'c1111111-0000-0000-0000-000000000001';

const adminUser = {
  personId: '1', email: 'admin@test.ch', role: 'ADMIN' as const,
  clubMemberships: [{ clubId: CLUB_ID, personId: '1', role: 'ADMIN' as const }],
} satisfies MockUser;

const memberUser = {
  personId: '2', email: 'member@test.ch', role: 'USER' as const,
  clubMemberships: [{ clubId: CLUB_ID, personId: '2', role: 'MEMBER' as const }],
} satisfies MockUser;

const club = { id: CLUB_ID, name: 'Test Club', location: 'Zurich' };
const teams = [{ id: 't1', name: 'Herren 1', clubId: CLUB_ID }];

function mockAuthFor(currentUserSig: WritableSignal<MockUser | null>) {
  return {
    currentUser: currentUserSig,
    isAdmin: signal(currentUserSig()?.role === 'ADMIN'),
    isClubAdmin(clubId: string): boolean {
      return currentUserSig()?.clubMemberships
        .some((m: ClubMembership) => m.clubId === clubId && m.role === 'ADMIN') ?? false;
    },
  };
}

describe('ClubDetailComponent', () => {
  describe('Bug 2 — delete button hides for club members', () => {
    let fixture: ComponentFixture<ClubDetailComponent>;
    let http: HttpTestingController;

    beforeEach(async () => {
      const currentUser = signal<MockUser | null>(memberUser);
      await TestBed.configureTestingModule({
        imports: [ClubDetailComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => CLUB_ID } } } },
          { provide: AuthService, useValue: mockAuthFor(currentUser) },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ClubDetailComponent);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('hides delete button for members', () => {
      fixture.detectChanges();
      http.expectOne(`/api/clubs/${CLUB_ID}`).flush(club);
      http.expectOne(`/api/clubs/${CLUB_ID}/teams`).flush(teams);
      fixture.detectChanges();

      const deleteButtons = fixture.nativeElement.querySelectorAll('.btn-danger');
      expect(deleteButtons.length).toBe(0);
    });
  });

  describe('Bug 2 — delete button visible for club admins', () => {
    let fixture: ComponentFixture<ClubDetailComponent>;
    let http: HttpTestingController;

    beforeEach(async () => {
      const currentUser = signal<MockUser | null>(adminUser);
      await TestBed.configureTestingModule({
        imports: [ClubDetailComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => CLUB_ID } } } },
          { provide: AuthService, useValue: mockAuthFor(currentUser) },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ClubDetailComponent);
      http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('shows delete button for admins', () => {
      fixture.detectChanges();
      http.expectOne(`/api/clubs/${CLUB_ID}`).flush(club);
      http.expectOne(`/api/clubs/${CLUB_ID}/teams`).flush(teams);
      fixture.detectChanges();

      const deleteButtons = fixture.nativeElement.querySelectorAll('.btn-danger');
      expect(deleteButtons.length).toBeGreaterThan(0);
    });
  });

  describe('Bug 3 — data reloads when user switches', () => {
    let fixture: ComponentFixture<ClubDetailComponent>;
    let http: HttpTestingController;
    let currentUser: WritableSignal<MockUser | null>;

    beforeEach(async () => {
      currentUser = signal<MockUser | null>(adminUser);
      await TestBed.configureTestingModule({
        imports: [ClubDetailComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => CLUB_ID } } } },
          { provide: AuthService, useValue: mockAuthFor(currentUser) },
        ],
      }).compileComponents();

      fixture = TestBed.createComponent(ClubDetailComponent);
      http = TestBed.inject(HttpTestingController);

      fixture.detectChanges();
      http.expectOne(`/api/clubs/${CLUB_ID}`).flush(club);
      http.expectOne(`/api/clubs/${CLUB_ID}/teams`).flush(teams);
      fixture.detectChanges();
    });

    afterEach(() => http.verify());

    it('reloads club and teams when current user changes', () => {
      currentUser.set(memberUser);
      fixture.detectChanges();

      const clubReq = http.expectOne(`/api/clubs/${CLUB_ID}`);
      expect(clubReq.request.method).toBe('GET');
      clubReq.flush(club);

      const teamsReq = http.expectOne(`/api/clubs/${CLUB_ID}/teams`);
      expect(teamsReq.request.method).toBe('GET');
      teamsReq.flush([]);
    });
  });
});
