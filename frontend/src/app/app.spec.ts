import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App shell', () => {
  it('renders when the app boots', async () => {
    await TestBed.configureTestingModule({ imports: [App] }).compileComponents();

    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });
});
