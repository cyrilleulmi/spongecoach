import { TestBed } from '@angular/core/testing';
import { ManageChips } from './manage-chips';

describe('ManageChips', () => {
  async function render() {
    await TestBed.configureTestingModule({ imports: [ManageChips] }).compileComponents();
    const fixture = TestBed.createComponent(ManageChips);
    fixture.componentRef.setInput('items', [
      { id: 'a', name: 'Passgenauigkeit', color: '#2c7a68' },
      { id: 'b', name: 'Schusshärte', color: '#c8722e' },
    ]);
    fixture.componentRef.setInput('associatedIds', new Set(['a']));
    await fixture.whenStable();
    return fixture;
  }

  it('renders one chip per catalog item, marking associated ones', async () => {
    const fixture = await render();
    const chips: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('.chip'));
    expect(chips).toHaveLength(2);
    expect(chips[0].classList.contains('on')).toBe(true);
    expect(chips[1].classList.contains('on')).toBe(false);
  });

  it('shows a remove affordance for an associated item and an add affordance otherwise', async () => {
    const fixture = await render();
    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.chip button.x'));
    expect(buttons[0].textContent?.trim()).toBe('×');
    expect(buttons[1].textContent?.trim()).toBe('+');
  });

  it('emits the item id when its chip button is clicked', async () => {
    const fixture = await render();
    const toggled: string[] = [];
    fixture.componentInstance.toggle.subscribe((id) => toggled.push(id));

    const buttons: HTMLButtonElement[] = Array.from(fixture.nativeElement.querySelectorAll('.chip button.x'));
    buttons[1].click();

    expect(toggled).toEqual(['b']);
  });
});
