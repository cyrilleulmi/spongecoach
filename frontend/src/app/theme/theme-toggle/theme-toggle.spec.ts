import { TestBed } from '@angular/core/testing';
import { ThemeToggle } from './theme-toggle';

describe('ThemeToggle', () => {
  afterEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['theme'];
  });

  async function render() {
    await TestBed.configureTestingModule({ imports: [ThemeToggle] }).compileComponents();
    const fixture = TestBed.createComponent(ThemeToggle);
    fixture.detectChanges();
    return fixture;
  }

  it('defaults to light and stamps it on <html>', async () => {
    const fixture = await render();

    expect(document.documentElement.dataset['theme']).toBe('light');
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    expect(button.getAttribute('aria-label')).toBe('Zu dunklem Design wechseln');
  });

  it('toggles to dark on click, persisting the choice', async () => {
    const fixture = await render();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button');

    button.click();
    fixture.detectChanges();

    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(localStorage.getItem('sc-theme')).toBe('dark');
    expect(button.getAttribute('aria-label')).toBe('Zu hellem Design wechseln');
  });

  it('restores a previously stored choice on init', async () => {
    localStorage.setItem('sc-theme', 'dark');

    await render();

    expect(document.documentElement.dataset['theme']).toBe('dark');
  });
});
