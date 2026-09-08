import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App shell', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('renders when the app boots', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();

    httpMock.expectOne('/api/lines').flush([]);
    httpMock.expectOne('/api/players').flush([]);
    httpMock.expectOne('/api/skills').flush([]);
    httpMock.expectOne('/api/development-goals').flush([]);
    httpMock.expectOne('/api/focuses').flush([]);
  });
});
