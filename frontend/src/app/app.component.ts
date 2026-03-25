import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigUploadComponent } from './components/config-upload/config-upload.component';
import { MtomAnalyzerComponent } from './components/mtom-analyzer/mtom-analyzer.component';
import { VisualMappingComponent } from './components/visual-mapping/visual-mapping.component';
import { ValidationPanelComponent } from './components/validation-panel/validation-panel.component';
import { ResultDisplayComponent } from './components/result-display/result-display.component';
import { ConverterService, ClientConfig, DetectedField, ConversionResult } from './services/converter.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    ConfigUploadComponent,
    MtomAnalyzerComponent,
    VisualMappingComponent,
    ValidationPanelComponent,
    ResultDisplayComponent
  ],
  template: `
    <div class="app-container">
      <!-- Header -->
      <header class="app-header">
        <div class="header-content">
          <div class="header-logo">BTE</div>
          <div class="header-text">
            <h1>MTOM-to-JSON Converter</h1>
            <p class="subtitle">Klantconfiguratie mapping tool voor Bulk Toevoer ECM</p>
          </div>
        </div>
      </header>

      <!-- Progress wizard -->
      <nav class="wizard-nav">
        <div *ngFor="let step of steps; let i = index; let last = last"
             class="wizard-step-wrapper">
          <div class="wizard-step"
               [class.active]="currentStep === i"
               [class.completed]="currentStep > i"
               [class.upcoming]="currentStep < i">
            <div class="step-circle">
              <span *ngIf="currentStep <= i">{{ i + 1 }}</span>
              <span *ngIf="currentStep > i">&#10004;</span>
            </div>
            <div class="step-info">
              <div class="step-label">{{ step.label }}</div>
              <div class="step-desc">{{ step.desc }}</div>
            </div>
          </div>
          <div *ngIf="!last" class="step-connector" [class.done]="currentStep > i"></div>
        </div>
      </nav>

      <!-- Main content -->
      <main class="app-main">
        <!-- Step 1: YAML Config -->
        <section class="step-section" [class.active-section]="currentStep === 0">
          <app-config-upload (configLoaded)="onConfigLoaded($event)"></app-config-upload>
        </section>

        <!-- Step 2: MTOM Upload & Analysis -->
        <section *ngIf="currentStep >= 1" class="step-section" [class.active-section]="currentStep === 1">
          <app-mtom-analyzer
            (fieldsDetected)="onFieldsDetected($event)"
            (fileSelected)="onMtomFileSelected($event)">
          </app-mtom-analyzer>
        </section>

        <!-- Step 3: Visual Mapping -->
        <section *ngIf="currentStep >= 2" class="step-section" [class.active-section]="currentStep === 2">
          <app-visual-mapping
            [detectedFields]="detectedFields"
            [configFields]="config?.metadataMapping?.fields || []">
          </app-visual-mapping>

          <div class="convert-action">
            <button class="convert-btn" (click)="convert()" [disabled]="converting">
              <span *ngIf="!converting">&#9654; Start Conversie &amp; Validatie</span>
              <span *ngIf="converting">&#8987; Bezig met converteren...</span>
            </button>
            <p class="convert-hint">Voert de MTOM-naar-JSON conversie uit met drielaagse validatie</p>
          </div>
        </section>

        <!-- Step 4: Validation -->
        <section *ngIf="conversionResult" class="step-section" [class.active-section]="currentStep === 3">
          <app-validation-panel
            [validationResults]="conversionResult.validationResults || []">
          </app-validation-panel>
        </section>

        <!-- Step 5: Result -->
        <section *ngIf="conversionResult" class="step-section" [class.active-section]="currentStep === 4">
          <app-result-display [result]="conversionResult"></app-result-display>
        </section>
      </main>

      <!-- Footer -->
      <footer class="app-footer">
        <span>BTE POC &mdash; MTOM-to-JSON Converter</span>
        <span class="footer-sep">|</span>
        <span>Belastingdienst &mdash; Bulk Toevoer ECM</span>
      </footer>
    </div>
  `,
  styles: [`
    * { box-sizing: border-box; }
    .app-container {
      max-width: 1280px; margin: 0 auto; padding: 0 2rem 2rem;
      font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif;
      color: #333;
    }

    /* Header */
    .app-header {
      background: linear-gradient(135deg, #0d47a1 0%, #1565c0 50%, #1976d2 100%);
      margin: 0 -2rem; padding: 1.5rem 2rem;
      color: white;
    }
    .header-content { display: flex; align-items: center; gap: 1rem; max-width: 1280px; margin: 0 auto; }
    .header-logo {
      width: 50px; height: 50px; border-radius: 10px;
      background: rgba(255,255,255,0.2); backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center;
      font-size: 1.2rem; font-weight: 800; letter-spacing: 1px;
    }
    .header-text h1 { margin: 0; font-size: 1.4rem; font-weight: 700; }
    .subtitle { margin: 0; font-size: 0.85rem; opacity: 0.8; }

    /* Wizard navigation */
    .wizard-nav {
      display: flex; align-items: flex-start; gap: 0;
      padding: 1.5rem 0; margin-bottom: 1rem;
      overflow-x: auto;
    }
    .wizard-step-wrapper { display: flex; align-items: center; }
    .wizard-step {
      display: flex; align-items: center; gap: 0.6rem;
      padding: 0.5rem 0.75rem; border-radius: 8px;
      transition: all 0.3s; white-space: nowrap; min-width: 140px;
    }
    .wizard-step.active { background: #e3f2fd; }
    .wizard-step.completed { opacity: 1; }
    .wizard-step.upcoming { opacity: 0.4; }

    .step-circle {
      width: 32px; height: 32px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 0.85rem; flex-shrink: 0;
      background: #e0e0e0; color: #999;
      transition: all 0.3s;
    }
    .wizard-step.active .step-circle { background: #1976d2; color: white; box-shadow: 0 2px 8px rgba(25,118,210,0.4); }
    .wizard-step.completed .step-circle { background: #4caf50; color: white; }

    .step-info { display: flex; flex-direction: column; }
    .step-label { font-weight: 600; font-size: 0.82rem; color: #333; }
    .step-desc { font-size: 0.7rem; color: #999; }
    .wizard-step.active .step-label { color: #1565c0; }
    .wizard-step.active .step-desc { color: #64b5f6; }

    .step-connector {
      width: 30px; height: 2px; background: #e0e0e0;
      margin: 0 4px; flex-shrink: 0; transition: background 0.3s;
    }
    .step-connector.done { background: #4caf50; }

    /* Sections */
    .step-section {
      padding: 1.5rem;
      border: 2px solid transparent;
      border-radius: 12px;
      margin-bottom: 1rem;
      transition: all 0.3s;
    }
    .step-section.active-section {
      border-color: #e3f2fd;
      background: #fafcff;
      box-shadow: 0 2px 12px rgba(0,0,0,0.04);
    }

    /* Convert button */
    .convert-action { text-align: center; margin-top: 2rem; }
    .convert-btn {
      padding: 1rem 3rem; font-size: 1.15rem; font-weight: 700;
      background: linear-gradient(135deg, #1976d2, #1565c0);
      color: white; border: none; border-radius: 10px;
      cursor: pointer; transition: all 0.3s;
      box-shadow: 0 4px 15px rgba(25,118,210,0.3);
    }
    .convert-btn:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(25,118,210,0.4);
    }
    .convert-btn:disabled { background: #bbb; box-shadow: none; cursor: not-allowed; }
    .convert-hint { font-size: 0.8rem; color: #999; margin-top: 0.5rem; }

    /* Footer */
    .app-footer {
      text-align: center; margin-top: 3rem; padding: 1.5rem 0;
      border-top: 1px solid #e0e0e0;
      font-size: 0.8rem; color: #999;
    }
    .footer-sep { margin: 0 0.75rem; }
  `]
})
export class AppComponent {
  steps = [
    { label: 'YAML Config', desc: 'Upload mapping' },
    { label: 'MTOM Analyse', desc: 'Bronbestand' },
    { label: 'Mapping', desc: 'MTOM naar JSON' },
    { label: 'Validatie', desc: 'Drielaags' },
    { label: 'Resultaat', desc: 'JSON output' }
  ];
  currentStep = 0;

  config: ClientConfig | null = null;
  detectedFields: DetectedField[] = [];
  mtomFile: File | null = null;
  conversionResult: ConversionResult | null = null;
  converting = false;

  constructor(private converterService: ConverterService) {}

  onConfigLoaded(config: ClientConfig) {
    this.config = config;
    this.currentStep = 1;
  }

  onFieldsDetected(fields: DetectedField[]) {
    this.detectedFields = fields;
    this.currentStep = 2;
  }

  onMtomFileSelected(file: File) {
    this.mtomFile = file;
  }

  convert() {
    if (!this.mtomFile) return;
    this.converting = true;
    this.conversionResult = null;

    this.converterService.convert(this.mtomFile).subscribe({
      next: (result) => {
        this.conversionResult = result;
        this.converting = false;
        this.currentStep = 3;
        // Auto-advance to result after a moment
        setTimeout(() => {
          if (this.conversionResult) {
            this.currentStep = 4;
          }
        }, 1500);
      },
      error: (err) => {
        this.converting = false;
        console.error('Conversion error:', err);
      }
    });
  }
}
