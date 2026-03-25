import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigUploadComponent } from './config/config-upload/config-upload.component';
import { MtomAnalyzerComponent } from './analyzer/mtom-analyzer/mtom-analyzer.component';
import { VisualMappingComponent } from './mapping/visual-mapping/visual-mapping.component';
import { ValidationPanelComponent } from './results/validation-panel/validation-panel.component';
import { ResultDisplayComponent } from './results/result-display/result-display.component';
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
  templateUrl: './app.component.html'
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
