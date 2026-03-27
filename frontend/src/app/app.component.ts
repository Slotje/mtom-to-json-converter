import { Component, CUSTOM_ELEMENTS_SCHEMA, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { WizardStateService } from './services/wizard-state.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet
  ],
  templateUrl: './app.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppComponent {
  constructor(
    private router: Router,
    public wizard: WizardStateService
  ) {}

  @HostListener('bldcRouter', ['$event'])
  handleRoute(event: any): void {
    this.router.navigateByUrl(event.detail.routerLink);
  }
}
