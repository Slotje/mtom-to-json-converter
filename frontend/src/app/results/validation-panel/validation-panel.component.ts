import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValidationResult } from '../../services/converter.service';

@Component({
  selector: 'app-validation-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './validation-panel.component.html'
})
export class ValidationPanelComponent implements OnChanges {
  @Input() validationResults: ValidationResult[] = [];

  expandedLayers: { [key: string]: boolean } = {};
  allValid = true;
  totalErrors = 0;
  totalWarnings = 0;

  ngOnChanges(changes: SimpleChanges) {
    this.totalErrors = this.validationResults.reduce((sum, r) => sum + r.errors.length, 0);
    this.totalWarnings = this.validationResults.reduce((sum, r) => sum + (r.warnings || []).length, 0);
    this.allValid = this.validationResults.every(r => r.valid);

    // Auto-expand layers with issues
    for (const result of this.validationResults) {
      if (!result.valid || (result.warnings || []).length > 0) {
        this.expandedLayers[result.layer] = true;
      }
    }
  }

  toggleLayer(layer: string) {
    this.expandedLayers[layer] = !this.expandedLayers[layer];
  }

  getLayerName(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'JSON Schema Validatie';
      case 'BUSINESS': return 'Business Rule Validatie';
      case 'REFERENCE': return 'Referentie-integriteit Validatie';
      default: return layer;
    }
  }

  getLayerShortName(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'Schema';
      case 'BUSINESS': return 'Business';
      case 'REFERENCE': return 'Referentie';
      default: return layer;
    }
  }

  getLayerDescription(layer: string): string {
    switch (layer) {
      case 'SCHEMA': return 'Controleert structuur, verplichte velden en datatypes van de configuratie';
      case 'BUSINESS': return 'Valideert business regels: dubbele mappings, formaten en veldaantallen';
      case 'REFERENCE': return 'Controleert of XPath expressies geldig zijn en datatypes overeenkomen';
      default: return '';
    }
  }
}
