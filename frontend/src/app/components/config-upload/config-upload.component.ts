import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ConverterService, ConfigUploadResponse, ClientConfig, ValidationResult } from '../../services/converter.service';

interface SampleConfig {
  label: string;
  file: string;
  description: string;
  icon: string;
  type: 'success' | 'error-schema' | 'error-business' | 'error-reference';
}

@Component({
  selector: 'app-config-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="config-upload">
      <h2>Stap 1: YAML Configuratie Uploaden</h2>

      <!-- Sample configs dropdown -->
      <div class="sample-section">
        <h3>Demo Configuraties</h3>
        <p class="hint">Kies een voorbeeldconfiguratie om de werking te demonstreren:</p>
        <div class="sample-cards">
          <div *ngFor="let sample of sampleConfigs"
               class="sample-card"
               [class.selected]="selectedSample === sample"
               [class]="'sample-card type-' + sample.type"
               (click)="loadSample(sample)">
            <div class="sample-icon">{{ sample.icon }}</div>
            <div class="sample-info">
              <div class="sample-label">{{ sample.label }}</div>
              <div class="sample-desc">{{ sample.description }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="divider">
        <span>of upload een eigen bestand</span>
      </div>

      <!-- File upload zone -->
      <div class="upload-zone"
           [class.dragover]="isDragOver"
           (dragover)="onDragOver($event)"
           (dragleave)="onDragLeave($event)"
           (drop)="onDrop($event)">
        <p>Sleep een YAML bestand hierheen of</p>
        <label class="upload-btn">
          Kies bestand
          <input type="file" accept=".yaml,.yml" (change)="onFileSelect($event)" hidden>
        </label>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-bar">
        <div class="loading-fill"></div>
      </div>

      <!-- Error -->
      <div *ngIf="error" class="error-message">
        <strong>Fout:</strong> {{ error }}
        <p *ngIf="suggestion" class="suggestion">Suggestie: {{ suggestion }}</p>
      </div>

      <!-- Config summary -->
      <div *ngIf="config" class="config-summary">
        <h3>Configuratie Overzicht</h3>
        <table class="config-table">
          <tr><td><strong>Client ID</strong></td><td>{{ config.clientId || '(leeg)' }}</td></tr>
          <tr><td><strong>Client Naam</strong></td><td>{{ config.clientName || '(leeg)' }}</td></tr>
          <tr><td><strong>Object Store</strong></td><td>{{ config.metadataMapping?.objectStore || '(leeg)' }}</td></tr>
          <tr><td><strong>Document Class</strong></td><td>{{ config.metadataMapping?.documentClass || '(leeg)' }}</td></tr>
          <tr><td><strong>Aantal velden</strong></td><td>{{ config.metadataMapping?.fields?.length || 0 }}</td></tr>
          <tr><td><strong>Contact Email</strong></td><td>{{ config.businessInfo?.contactEmail || '(leeg)' }}</td></tr>
          <tr><td><strong>Support Groep</strong></td><td>{{ config.businessInfo?.supportGroup || '(leeg)' }}</td></tr>
        </table>

        <h4>Veldmappings</h4>
        <table class="fields-table">
          <thead>
            <tr>
              <th>Bronveld (XPath)</th>
              <th>Doelveld</th>
              <th>Type</th>
              <th>Verplicht</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let field of config.metadataMapping?.fields">
              <td><code>{{ field.sourceField || '(leeg)' }}</code></td>
              <td>{{ field.targetProperty || '(leeg)' }}</td>
              <td><span class="type-badge">{{ field.dataType }}</span></td>
              <td>
                <span [class]="field.required ? 'badge required' : 'badge optional'">
                  {{ field.required ? 'Ja' : 'Nee' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Inline validation result -->
        <div *ngIf="validation" class="validation-inline">
          <div [class]="'validation-status ' + (validation.valid ? 'valid' : 'invalid')">
            {{ validation.valid ? 'Schema validatie geslaagd' : 'Schema validatie: fouten gevonden' }}
          </div>
          <div *ngFor="let err of validation.errors" class="validation-error">
            <span class="error-code">{{ err.code }}</span>
            <span class="error-msg">{{ err.message }}</span>
            <div class="error-suggestion">{{ err.suggestion }}</div>
          </div>
          <div *ngFor="let warn of (validation.warnings || [])" class="validation-warning">
            <span class="error-code">{{ warn.code }}</span>
            <span class="error-msg">{{ warn.message }}</span>
            <div class="error-suggestion">{{ warn.suggestion }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .config-upload { margin-bottom: 1rem; }

    /* Sample section */
    .sample-section { margin-bottom: 1.5rem; }
    .sample-section h3 { margin-bottom: 0.25rem; color: #333; }
    .hint { color: #666; font-size: 0.85rem; margin-bottom: 0.75rem; }
    .sample-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0.75rem; }
    .sample-card {
      display: flex; align-items: center; gap: 0.75rem;
      padding: 0.75rem 1rem; border-radius: 8px; cursor: pointer;
      border: 2px solid #e0e0e0; transition: all 0.2s;
      background: white;
    }
    .sample-card:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .sample-card.selected { border-width: 3px; }
    .type-success { border-color: #a5d6a7; }
    .type-success.selected { border-color: #4caf50; background: #f1f8e9; }
    .type-error-schema { border-color: #ef9a9a; }
    .type-error-schema.selected { border-color: #f44336; background: #fce4ec; }
    .type-error-business { border-color: #ffcc80; }
    .type-error-business.selected { border-color: #ff9800; background: #fff3e0; }
    .type-error-reference { border-color: #ce93d8; }
    .type-error-reference.selected { border-color: #9c27b0; background: #f3e5f5; }
    .sample-icon { font-size: 1.8rem; flex-shrink: 0; }
    .sample-info { flex: 1; min-width: 0; }
    .sample-label { font-weight: 700; font-size: 0.9rem; color: #333; }
    .sample-desc { font-size: 0.75rem; color: #666; }

    /* Divider */
    .divider {
      display: flex; align-items: center; gap: 1rem;
      margin: 1.5rem 0; color: #999; font-size: 0.85rem;
    }
    .divider::before, .divider::after {
      content: ''; flex: 1; height: 1px; background: #e0e0e0;
    }

    /* Upload zone */
    .upload-zone {
      border: 2px dashed #ccc; border-radius: 8px;
      padding: 1.5rem; text-align: center; transition: all 0.3s; background: #fafafa;
    }
    .upload-zone.dragover { border-color: #1976d2; background: #e3f2fd; }
    .upload-btn {
      display: inline-block; padding: 0.5rem 1.5rem;
      background: #1976d2; color: white; border-radius: 4px;
      cursor: pointer; margin-top: 0.5rem;
    }
    .upload-btn:hover { background: #1565c0; }

    /* Loading bar */
    .loading-bar {
      height: 4px; background: #e0e0e0; border-radius: 2px; margin-top: 1rem; overflow: hidden;
    }
    .loading-fill {
      height: 100%; width: 30%; background: #1976d2; border-radius: 2px;
      animation: loading 1s ease-in-out infinite;
    }
    @keyframes loading {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(400%); }
    }

    /* Tables */
    .config-table, .fields-table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
    .config-table td, .fields-table th, .fields-table td { padding: 0.5rem; border: 1px solid #e0e0e0; }
    .fields-table th { background: #f5f5f5; text-align: left; }
    .type-badge {
      display: inline-block; padding: 2px 8px; border-radius: 12px;
      font-size: 0.8rem; background: #e3f2fd; color: #1565c0;
    }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.8rem; }
    .badge.required { background: #ffebee; color: #c62828; }
    .badge.optional { background: #e8f5e9; color: #2e7d32; }

    /* Error / validation */
    .error-message { background: #ffebee; padding: 1rem; border-radius: 4px; margin-top: 1rem; color: #c62828; }
    .suggestion { color: #555; font-style: italic; margin-top: 0.5rem; }
    .config-summary { margin-top: 1.5rem; }
    .validation-inline { margin-top: 1rem; }
    .validation-status { padding: 0.5rem 1rem; border-radius: 4px; font-weight: bold; }
    .validation-status.valid { background: #e8f5e9; color: #2e7d32; }
    .validation-status.invalid { background: #ffebee; color: #c62828; }
    .validation-error {
      background: #fff3e0; padding: 0.5rem; margin-top: 0.5rem; border-radius: 4px;
    }
    .validation-warning {
      background: #fff8e1; padding: 0.5rem; margin-top: 0.5rem; border-radius: 4px;
    }
    .error-code { font-weight: bold; color: #e65100; margin-right: 0.5rem; }
    .error-msg { }
    .error-suggestion { color: #666; font-style: italic; margin-top: 0.25rem; padding-left: 1rem; font-size: 0.85rem; }

    @media (max-width: 700px) {
      .sample-cards { grid-template-columns: 1fr; }
    }
  `]
})
export class ConfigUploadComponent {
  @Output() configLoaded = new EventEmitter<ClientConfig>();

  sampleConfigs: SampleConfig[] = [
    {
      label: 'Correcte Mapping',
      file: 'assets/samples/correct-mapping.yaml',
      description: 'Volledige correcte configuratie met alle 11+ verplichte DNI/IVAA velden',
      icon: '\u2705',
      type: 'success'
    },
    {
      label: 'Fout: Schema Validatie (Laag 1)',
      file: 'assets/samples/error-schema-validation.yaml',
      description: 'Ontbrekende verplichte velden, lege clientId, ongeldig datatype, fout email',
      icon: '\u274C',
      type: 'error-schema'
    },
    {
      label: 'Fout: Business Rules (Laag 2)',
      file: 'assets/samples/error-business-rules.yaml',
      description: 'Dubbele targetProperty, ongeldig datumformaat, te weinig velden',
      icon: '\u26A0\uFE0F',
      type: 'error-business'
    },
    {
      label: 'Fout: Referentie-integriteit (Laag 3)',
      file: 'assets/samples/error-reference-integrity.yaml',
      description: 'Type-incompatibiliteit (string als integer), niet-bestaand XPath veld',
      icon: '\uD83D\uDD17',
      type: 'error-reference'
    }
  ];

  selectedSample: SampleConfig | null = null;
  isDragOver = false;
  loading = false;
  config: ClientConfig | null = null;
  validation: ValidationResult | null = null;
  error: string | null = null;
  suggestion: string | null = null;

  constructor(
    private converterService: ConverterService,
    private http: HttpClient
  ) {}

  loadSample(sample: SampleConfig) {
    this.selectedSample = sample;
    this.loading = true;
    this.error = null;
    this.config = null;
    this.validation = null;

    this.http.get(sample.file, { responseType: 'text' }).subscribe({
      next: (content) => {
        this.uploadYaml(content);
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Kon sample bestand niet laden: ' + sample.file;
      }
    });
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.processFile(file);
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.processFile(file);
  }

  private processFile(file: File) {
    this.loading = true;
    this.error = null;
    this.selectedSample = null;
    const reader = new FileReader();
    reader.onload = () => this.uploadYaml(reader.result as string);
    reader.readAsText(file);
  }

  private uploadYaml(content: string) {
    this.converterService.uploadConfig(content).subscribe({
      next: (response) => {
        this.loading = false;
        this.config = response.config;
        this.validation = response.validation;
        // Always emit config (even with validation errors) so we can show the full flow
        this.configLoaded.emit(response.config);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Onbekende fout bij het uploaden';
        this.suggestion = err.error?.suggestion;
      }
    });
  }
}
