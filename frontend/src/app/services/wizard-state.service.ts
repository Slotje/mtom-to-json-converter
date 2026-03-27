import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { ClientConfig, DetectedField, ConversionResult } from './converter.service';

@Injectable({ providedIn: 'root' })
export class WizardStateService {
  config: ClientConfig | null = null;
  detectedFields: DetectedField[] = [];
  mtomFile: File | null = null;
  conversionResult: ConversionResult | null = null;
  converting = false;

  steps = [
    { label: 'YAML Config', desc: 'Upload mapping', route: '/config' },
    { label: 'MTOM Analyse', desc: 'Bronbestand', route: '/analyze' },
    { label: 'Mapping', desc: 'MTOM naar JSON', route: '/mapping' },
    { label: 'Validatie', desc: 'Drielaags', route: '/validation' },
    { label: 'Resultaat', desc: 'JSON output', route: '/result' }
  ];

  constructor(private router: Router) {}

  get currentStepIndex(): number {
    const currentRoute = this.router.url;
    const index = this.steps.findIndex(s => s.route === currentRoute);
    return index >= 0 ? index : 0;
  }

  resetFromStep(stepIndex: number) {
    if (stepIndex <= 1) {
      this.detectedFields = [];
      this.mtomFile = null;
    }
    if (stepIndex <= 2) {
      this.conversionResult = null;
      this.converting = false;
    }
  }

  resetAll() {
    this.config = null;
    this.detectedFields = [];
    this.mtomFile = null;
    this.conversionResult = null;
    this.converting = false;
  }

  navigateTo(route: string) {
    this.router.navigateByUrl(route);
  }
}
