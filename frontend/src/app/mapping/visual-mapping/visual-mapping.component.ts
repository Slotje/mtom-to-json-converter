import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DetectedField, FieldMapping } from '../../services/converter.service';
import { WizardStateService } from '../../services/wizard-state.service';
import { ConverterService } from '../../services/converter.service';

interface MappingConnection {
  source: DetectedField;
  target: FieldMapping;
  matched: boolean;
}

@Component({
  selector: 'app-visual-mapping',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visual-mapping.component.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class VisualMappingComponent implements OnInit {
  detectedFields: DetectedField[] = [];
  configFields: FieldMapping[] = [];

  connections: MappingConnection[] = [];
  unmappedRequired: FieldMapping[] = [];
  unmappedOptional: FieldMapping[] = [];
  unmappedSourceFields: DetectedField[] = [];
  converting = false;

  constructor(
    public wizard: WizardStateService,
    private converterService: ConverterService
  ) {}

  ngOnInit() {
    this.detectedFields = this.wizard.detectedFields;
    this.configFields = this.wizard.config?.metadataMapping?.fields || [];
    this.autoMap();
  }

  convert() {
    if (!this.wizard.mtomFile) return;
    this.converting = true;

    this.converterService.convert(this.wizard.mtomFile).subscribe({
      next: (result) => {
        this.wizard.conversionResult = result;
        this.converting = false;
        this.wizard.navigateTo('/validation');
      },
      error: (err) => {
        this.converting = false;
        console.error('Conversion error:', err);
      }
    });
  }

  private autoMap() {
    this.connections = [];
    const mappedSources = new Set<DetectedField>();

    for (const configField of this.configFields) {
      const match = this.detectedFields.find(df => {
        const normalizedSource = configField.sourceField.toLowerCase();
        const normalizedDetected = df.xpathExpression.toLowerCase();
        const fieldName = df.fieldName.toLowerCase();

        if (normalizedSource === normalizedDetected) return true;

        const keyMatch = normalizedSource.match(/@key='([^']+)'/);
        if (keyMatch && keyMatch[1].toLowerCase() === fieldName) return true;

        const elemName = normalizedSource.replace('//', '').toLowerCase();
        if (elemName === fieldName) return true;

        return false;
      });

      if (match) {
        this.connections.push({ source: match, target: configField, matched: true });
        mappedSources.add(match);
      }
    }

    this.unmappedRequired = this.configFields.filter(
      f => f.required && !this.connections.some(c => c.target === f)
    );

    this.unmappedOptional = this.configFields.filter(
      f => !f.required && !this.connections.some(c => c.target === f)
    );

    this.unmappedSourceFields = this.detectedFields.filter(f => !mappedSources.has(f));
  }
}
