import { Component } from '@angular/core';
import { LineOverview } from './lines/line-overview/line-overview';

@Component({
  imports: [LineOverview],
  selector: 'app-root',
  template: '<app-line-overview />',
})
export class App {}
