import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ConverterService, ClientConfig, ValidationResult } from '../../services/converter.service';
import { WizardStateService } from '../../services/wizard-state.service';

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
  templateUrl: './config-upload.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ConfigUploadComponent {
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
  loading = false;
  config: ClientConfig | null = null;
  validation: ValidationResult | null = null;
  error: string | null = null;
  suggestion: string | null = null;

  constructor(
    private converterService: ConverterService,
    private http: HttpClient,
    private wizard: WizardStateService
  ) {}

  loadSample(sample: SampleConfig) {
    this.selectedSample = sample;
    this.loading = true;
    this.error = null;
    this.config = null;
    this.validation = null;
    this.wizard.resetFromStep(0);

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

  private uploadYaml(content: string) {
    this.converterService.uploadConfig(content).subscribe({
      next: (response) => {
        this.loading = false;
        this.config = response.config;
        this.validation = response.validation;
        this.wizard.config = response.config;
        this.wizard.navigateTo('/analyze');
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Onbekende fout bij het uploaden';
        this.suggestion = err.error?.suggestion;
      }
    });
  }
}
